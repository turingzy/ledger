package cn.turingzy.ledger.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import cn.turingzy.ledger.data.transfer.BillFileCodec
import cn.turingzy.ledger.data.transfer.LedgerBackupValidator
import cn.turingzy.ledger.data.transfer.readBytesLimited
import cn.turingzy.ledger.database.LedgerDatabase
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.model.*
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class DefaultSettingsDataRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: LedgerDatabase,
    private val appearances: LedgerCardAppearanceRepository,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : SettingsDataRepository {
    private val json = Json { encodeDefaults = true }

    override suspend fun ensureDefaultRules() = withContext(ioDispatcher) {
        database.withTransaction {
            if (database.parseRuleDao().backupRows().isNotEmpty()) return@withTransaction
            BillParseRule.defaults().forEach { rule ->
                database.parseRuleDao().insertRule(ParseRule(name = rule.name, startRow = rule.startRow.toInt(),
                    mappingConfig = json.encodeToString(rule), isSystem = true).asEntity())
            }
        }
    }

    override suspend fun saveTemplate(template: CardTemplate): Long = withContext(ioDispatcher) {
        require(template.name.isNotBlank() && template.coverUri.isNotBlank()) { "请填写卡面名称并选择图片" }
        val stable = template.copy(id = template.id.coerceAtLeast(0), coverUri = storeImage(template.coverUri))
        if (stable.id == 0L) database.cardTemplateDao().insertTemplate(stable.asEntity())
        else { database.cardTemplateDao().updateTemplate(stable.asEntity()); stable.id }
    }

    override suspend fun copyImage(uri: Uri): String = withContext(ioDispatcher) { storeImage(uri.toString()) }

    private fun storeImage(value: String): String {
        val uri = Uri.parse(value)
        if (uri.scheme != "content") return value
        val data = requireNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytesLimited(8 * 1024 * 1024) }
        return writeImage(data)
    }

    private fun writeImage(bytes: ByteArray): String {
        require(bytes.size <= 8 * 1024 * 1024) { "图片不能超过 8 MB" }
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..16000 && bounds.outHeight in 1..16000) { "图片格式或尺寸无效" }
        val options = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 2048) inSampleSize *= 2
        }
        val bitmap = requireNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)) { "无法读取图片" }
        val file = File(context.filesDir, "card-${UUID.randomUUID()}.img")
        try {
            file.outputStream().use { output ->
                val format = if (bitmap.hasAlpha()) android.graphics.Bitmap.CompressFormat.PNG else android.graphics.Bitmap.CompressFormat.JPEG
                check(bitmap.compress(format, 92, output)) { "无法保存图片" }
            }
        } catch (error: Exception) {
            if (file.exists() && !file.delete()) timber.log.Timber.w("Unable to remove incomplete card image")
            throw error
        } finally { bitmap.recycle() }
        return Uri.fromFile(file).toString()
    }

    override suspend fun exportBills(uri: Uri, excel: Boolean, start: Long, end: Long): Int = withContext(ioDispatcher) {
        val details = database.transactionDao().getPopulatedTransactionsBetween(start, end).first().map { it.asExternalModel() }
        val header = listOf("交易时间", "金额", "收支类型", "分类", "备注", "账户", "转入账户", "币种")
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
        val rows = listOf(header) + details.map { detail ->
            val transaction = detail.transaction
            listOf(dateFormat.format(Instant.ofEpochMilli(transaction.transactionTime)),
                BigDecimal.valueOf(transaction.amount, 2).toPlainString(),
                when (transaction.type) { TransactionType.EXPENSE -> "支出"; TransactionType.INCOME -> "收入"; TransactionType.TRANSFER -> "转账" },
                detail.category.category.name, transaction.note.orEmpty(), detail.account.name, detail.toAccount?.name.orEmpty(), detail.account.currency)
        }
        val bytes = if (excel) BillFileCodec.workbook(rows) else BillFileCodec.csv(rows)
        write(uri, bytes)
        details.size
    }

    override suspend fun importBills(uri: Uri, rule: BillParseRule, selectedAccountId: Long?): Int = withContext(ioDispatcher) {
        require(rule.valid) { "解析规则无效" }
        val rows = BillFileCodec.read(read(uri, BillFileCodec.MAX_BYTES))
        val accounts = database.accountDao().backupRows().map { it.asExternalModel() }.filterNot { it.isDeleted }
        val categories = database.categoryDao().backupRows().map { it.asExternalModel() }.filterNot { it.isDeleted }
        val pending = cn.turingzy.ledger.data.transfer.BillImportPlanner.plan(rows, rule, selectedAccountId, accounts, categories)
        database.withTransaction {
            val balances = database.accountDao().backupRows().associate { it.accountId to it.currentBalance }.toMutableMap()
            pending.forEach { transaction ->
                currentCoroutineContext().ensureActive()
                fun add(id: Long, delta: Long) { balances[id] = Math.addExact(balances.getValue(id), delta) }
                check(database.accountDao().getAccountByIdOnce(transaction.accountId) != null) { "导入账户已删除" }
                check(database.categoryDao().getCategoryByIdOnce(transaction.categoryId) != null) { "导入分类已删除" }
                if (transaction.type == TransactionType.INCOME) add(transaction.accountId, transaction.amount)
                else add(transaction.accountId, -transaction.amount)
                transaction.toAccountId?.let { id ->
                    check(database.accountDao().getAccountByIdOnce(id) != null) { "转入账户已删除" }
                    add(id, transaction.amount)
                }
                database.transactionDao().insertTransaction(transaction.asEntity())
            }
            balances.forEach { (id, balance) ->
                val account = database.accountDao().getAccountByIdOnce(id)
                if (account != null && account.currentBalance != balance) database.accountDao().updateAccount(account.copy(currentBalance = balance))
            }
        }
        pending.size
    }

    override suspend fun backup(uri: Uri, includeBills: Boolean, includeSettings: Boolean) = withContext(ioDispatcher) {
        require(includeBills || includeSettings)
        val snapshot = database.withTransaction {
            val templates = database.cardTemplateDao().backupRows().map { it.asExternalModel() }
            LedgerBackup(
                createdAt = System.currentTimeMillis(),
                bills = if (includeBills) LedgerBillSnapshot(
                    database.accountDao().backupRows().map { it.asExternalModel() },
                    database.transactionDao().backupRows().map { it.asExternalModel() },
                    database.categoryDao().backupRows().map { it.asExternalModel() },
                    database.iconDao().backupRows().map { it.asExternalModel() }, templates) else null,
                settings = if (includeSettings) LedgerSettingsSnapshot(database.parseRuleDao().backupRows().map { it.asExternalModel() }, templates) else null,
            )
        }
        val appearanceData = if (includeSettings) SummaryCardTarget.entries.map { target ->
            val appearance = appearances.appearance(target).first()
            BackupAppearance(target, appearance.transparencyPercent, appearance.backgroundFile?.let {
                Base64.encodeToString(File(context.filesDir, it).inputStream().use { input -> input.readBytesLimited(8 * 1024 * 1024) }, Base64.NO_WRAP)
            })
        } else emptyList()
        fun pack(template: CardTemplate) = template.copy(coverUri = packImage(template.coverUri))
        val packed = snapshot.copy(
            bills = snapshot.bills?.copy(templates = snapshot.bills.templates.map(::pack)),
            settings = snapshot.settings?.copy(templates = snapshot.settings.templates.map(::pack), appearances = appearanceData),
        )
        LedgerBackupValidator.validate(packed)
        val bytes = json.encodeToString(packed).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BACKUP_BYTES) { "备份文件超过 128 MB" }
        write(uri, bytes)
    }

    override suspend fun restore(uri: Uri): Unit = withContext(ioDispatcher) {
        val backup = json.decodeFromString<LedgerBackup>(read(uri, MAX_BACKUP_BYTES).toString(Charsets.UTF_8))
        LedgerBackupValidator.validate(backup)
        val unpackedImages = mutableMapOf<String, String>()
        fun unpack(template: CardTemplate) = template.copy(coverUri = unpackedImages.getOrPut(template.coverUri) { unpackImage(template.coverUri) })
        val bills = backup.bills?.let { it.copy(templates = it.templates.map(::unpack)) }
        val settings = backup.settings?.let { it.copy(templates = it.templates.map(::unpack)) }
        // Keep rollback copies because applying a background removes its previous image.
        val prepared = mutableMapOf<SummaryCardTarget, String?>()
        val previous = mutableMapOf<SummaryCardTarget, Pair<Int, String?>>()
        var appearanceChanged = false
        try {
            settings?.appearances.orEmpty().forEach { appearance ->
                prepared[appearance.target] = appearance.image?.let { encoded ->
                    val uriString = writeImage(Base64.decode(encoded, Base64.DEFAULT))
                    try { appearances.prepareBackground(Uri.parse(uriString), appearance.target) }
                    finally {
                        val temporary = File(requireNotNull(Uri.parse(uriString).path))
                        if (!temporary.delete()) timber.log.Timber.w("Unable to remove staged restore image")
                    }
                }
                val old = appearances.appearance(appearance.target).first()
                val oldImage = old.backgroundFile?.let { filename ->
                    appearances.prepareBackground(Uri.fromFile(File(context.filesDir, filename)), appearance.target)
                }
                previous[appearance.target] = old.transparencyPercent to oldImage
            }
            database.withTransaction {
                val previousRules = if (bills != null && settings == null) database.parseRuleDao().backupRows() else emptyList()
                if (bills != null) {
                    database.parseRuleDao().clearRows()
                    database.transactionDao().clearRows()
                    database.accountDao().clearRows()
                    database.categoryDao().clearRows()
                    database.iconDao().clearRows()
                    database.cardTemplateDao().clearRows()
                    bills.icons.forEach { database.iconDao().insertIcon(it.asEntity()) }
                    bills.templates.forEach { database.cardTemplateDao().insertTemplate(it.asEntity()) }
                    bills.categories.forEach { database.categoryDao().insertCategory(it.asEntity()) }
                    bills.accounts.forEach { database.accountDao().insertAccount(it.asEntity()) }
                    bills.transactions.forEach { database.transactionDao().insertTransaction(it.asEntity()) }
                    previousRules.forEach { rule -> database.parseRuleDao().insertRule(rule.copy(defaultAccountId = null, defaultCategoryId = null)) }
                }
                if (settings != null) {
                    database.parseRuleDao().clearRows()
                    if (bills == null) {
                        val existing = database.cardTemplateDao().backupRows().map { it.templateId }.toSet()
                        val restoredIds = settings.templates.map { it.id }.toSet()
                        (existing - restoredIds).forEach { database.cardTemplateDao().softDeleteTemplate(it) }
                        settings.templates.forEach { template ->
                            if (template.id in existing) database.cardTemplateDao().updateTemplate(template.asEntity())
                            else database.cardTemplateDao().insertTemplate(template.asEntity())
                        }
                    }
                    settings.rules.forEach { rule ->
                        val restored = if (bills == null) rule.copy(defaultAccountId = null, defaultCategoryId = null) else rule
                        database.parseRuleDao().insertRule(restored.asEntity())
                    }
                }
                settings?.appearances?.forEach { appearance ->
                    appearanceChanged = true
                    val image = prepared[appearance.target]
                    if (image != null) appearances.applyPreparedBackground(image, appearance.target) else appearances.resetBackground(appearance.target)
                    appearances.setTransparencyPercent(appearance.transparency, appearance.target)
                }
            }
        } catch (error: Exception) {
            if (appearanceChanged) withContext(NonCancellable) {
                previous.forEach { (target, old) ->
                    try {
                        val image = old.second
                        if (image != null) appearances.applyPreparedBackground(image, target) else appearances.resetBackground(target)
                        appearances.setTransparencyPercent(old.first, target)
                    } catch (rollbackError: Exception) {
                        if (rollbackError is kotlinx.coroutines.CancellationException) throw rollbackError
                        timber.log.Timber.e(rollbackError, "Unable to roll back restored appearance")
                        error.addSuppressed(rollbackError)
                    }
                }
            }
            throw error
        } finally {
            withContext(NonCancellable) {
                (prepared.values + previous.values.map { it.second }).filterNotNull().forEach { appearances.discardPreparedBackground(it) }
            }
        }
    }

    private fun packImage(value: String): String {
        val uri = Uri.parse(value)
        if (uri.scheme !in setOf("file", "content")) return value
        val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytesLimited(8 * 1024 * 1024) }
        return "data:image;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun unpackImage(value: String): String = when {
        value.startsWith("data:image;base64,") -> writeImage(Base64.decode(value.substringAfter(','), Base64.DEFAULT))
        value.startsWith("res://") -> value
        else -> error("备份中包含无效的卡面图片")
    }

    private fun read(uri: Uri, limit: Int) = requireNotNull(context.contentResolver.openInputStream(uri)) { "无法读取文件" }
        .use { it.readBytesLimited(limit) }

    private suspend fun write(uri: Uri, bytes: ByteArray) {
        currentCoroutineContext().ensureActive()
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "无法写入文件" }.use { it.write(bytes) }
    }

    private companion object { const val MAX_BACKUP_BYTES = 128 * 1024 * 1024 }
}
