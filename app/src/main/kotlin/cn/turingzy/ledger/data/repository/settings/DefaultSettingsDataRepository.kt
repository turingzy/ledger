package cn.turingzy.ledger.data.repository.settings

import cn.turingzy.ledger.data.database.seed.ensureBuiltInCardFaces
import cn.turingzy.ledger.model.account.BuiltInCardFace
import cn.turingzy.ledger.data.repository.currency.ExchangeRateRepository
import cn.turingzy.ledger.data.repository.currency.MoneyValuation
import cn.turingzy.ledger.model.backup.BackupAppearance
import cn.turingzy.ledger.model.backup.BackupExchangeRate
import cn.turingzy.ledger.model.transfer.BillImportResult
import cn.turingzy.ledger.model.transfer.BillParseRule
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.backup.LedgerBackup
import cn.turingzy.ledger.model.backup.LedgerBillSnapshot
import cn.turingzy.ledger.model.backup.LedgerSettingsSnapshot
import cn.turingzy.ledger.model.transfer.ParseRule
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import cn.turingzy.ledger.data.transfer.BillFileCodec
import cn.turingzy.ledger.data.transfer.planBillImport
import cn.turingzy.ledger.data.transfer.LedgerBackupValidator
import cn.turingzy.ledger.data.transfer.readBytesLimited
import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
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

/**
 * [SettingsDataRepository] 的默认实现。
 *
 * 结合 Room 数据库、文件系统、偏好设置和多币种估值服务，
 * 负责系统默认规则填充、卡面模板图片管理、账单导出（CSV/Excel）、
 * 账单导入规划与执行、以及完整/部分备份与恢复（含故障回滚）。
 *
 * @param context 应用程序上下文
 * @param database 账本本地数据库
 * @param appearances 卡片外观仓库
 * @param preferences 用户偏好设置仓库
 * @param valuation 货币估值计算服务
 * @param exchangeRates 汇率仓库
 * @param ioDispatcher 用于执行 IO、编解码与数据库操作的协程调度器
 */
@Singleton
class DefaultSettingsDataRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: LedgerDatabase,
    private val appearances: LedgerCardAppearanceRepository,
    private val preferences: AppPreferencesRepository,
    private val valuation: MoneyValuation,
    private val exchangeRates: ExchangeRateRepository,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : SettingsDataRepository {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    /**
     * 若数据库中尚无任何导入规则，则在事务中插入内置的默认账单解析规则。
     */
    override suspend fun ensureDefaultRules() = withContext(ioDispatcher) {
        database.withTransaction {
            if (database.parseRuleDao().backupRows().isNotEmpty()) return@withTransaction
            BillParseRule.defaults().forEach { rule ->
                database.parseRuleDao().insertRule(ParseRule(name = rule.name, startRow = rule.startRow.toInt(),
                    mappingConfig = json.encodeToString(rule), isSystem = true).asEntity())
            }
        }
    }

    /**
     * 保存卡面模板。
     *
     * 如果模板图片尚未本地化，则会先将其拷贝并压缩至应用私有目录，随后持久化到数据库。
     *
     * @param template 待保存的卡面模板
     * @return 模板 ID
     */
    override suspend fun saveTemplate(template: CardTemplate): Long = withContext(ioDispatcher) {
        require(template.name.isNotBlank() && template.coverUri.isNotBlank()) { "请填写卡面名称并选择图片" }
        val normalized = BuiltInCardFace.byId(template.id)?.asTemplate(template)
            ?: template.copy(id = template.id.coerceAtLeast(0))
        val stable = normalized.copy(coverUri = storeImage(normalized.coverUri))
        if (stable.id == 0L) database.cardTemplateDao().insertTemplate(stable.asEntity())
        else { database.cardTemplateDao().updateTemplate(stable.asEntity()); stable.id }
    }

    /**
     * 将指定 URI 的图片转存至应用私有目录并返回文件路径 URI。
     *
     * @param uri 图片源 URI
     * @return 私有存储中的文件 URI 字符串
     */
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

    /**
     * 将指定时间范围内的交易导出为 CSV 或 Excel 文件并写入目标 URI。
     *
     * @param uri 目标写入 URI
     * @param excel 为 true 导出为 Excel 格式，否则导出为 CSV 格式
     * @param start 导出区间开始时间戳（毫秒）
     * @param end 导出区间结束时间戳（毫秒）
     * @return 导出的交易笔数
     */
    override suspend fun exportBills(uri: Uri, excel: Boolean, start: Long, end: Long): Int = withContext(ioDispatcher) {
        val details = database.transactionDao().getPopulatedTransactionsBetween(start, end).first().map { it.asExternalModel() }
        val header = listOf("交易时间", "金额", "收支类型", "分类", "备注", "账户", "转入账户", "币种", "来源交易号", "转入金额", "主要货币", "主要货币金额", "主要货币汇率", "汇率日期")
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
        val rows = listOf(header) + details.map { detail ->
            val transaction = detail.transaction
            listOf(dateFormat.format(Instant.ofEpochMilli(transaction.transactionTime)),
                BigDecimal.valueOf(transaction.amount, 2).toPlainString(),
                when (transaction.type) { TransactionType.EXPENSE -> "支出"; TransactionType.INCOME -> "收入"; TransactionType.TRANSFER -> "转账" },
                detail.category.category.name, transaction.note.orEmpty(), detail.account.name, detail.toAccount?.name.orEmpty(), detail.account.currency, transaction.importKey.orEmpty(),
                transaction.toAmount?.let { BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty(), transaction.baseCurrency.orEmpty(),
                transaction.baseAmount?.let { BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty(), transaction.baseRate.orEmpty(), transaction.baseRateDate.orEmpty())
        }
        val bytes = if (excel) BillFileCodec.workbook(rows) else BillFileCodec.csv(rows)
        write(uri, bytes)
        details.size
    }

    private suspend fun readImport(uri: Uri, rule: BillParseRule, selectedAccountId: Long?): List<Transaction> {
        require(rule.valid) { "解析规则无效" }
        val rows = BillFileCodec.read(read(uri, BillFileCodec.MAX_BYTES))
        val accounts = database.accountDao().backupRows().map { it.asExternalModel() }.filterNot { it.isDeleted }
        val categories = database.categoryDao().backupRows().map { it.asExternalModel() }.filterNot { it.isDeleted }
        return cn.turingzy.ledger.data.transfer.BillImportPlanner.plan(rows, rule, selectedAccountId, accounts, categories)
    }

    /**
     * 预解析账单并生成导入规划，返回有效条数、确定重复条数与疑似重复条数，不写入数据库。
     *
     * @param uri 账单文件 URI
     * @param rule 账单解析规则
     * @param selectedAccountId 默认目标账户 ID（可选）
     */
    override suspend fun previewImport(uri: Uri, rule: BillParseRule, selectedAccountId: Long?): BillImportResult = withContext(ioDispatcher) {
        val plan = planBillImport(readImport(uri, rule, selectedAccountId), database.transactionDao().backupRows().map { it.asExternalModel() }, false)
        BillImportResult(plan.transactions.size, plan.duplicates, plan.possibleDuplicates)
    }

    /**
     * 解析账单并将其正式导入数据库。
     *
     * 在事务中执行去重、多币种交易估值、原子更新账户余额及插入交易记录。
     *
     * @param uri 账单文件 URI
     * @param rule 账单解析规则
     * @param selectedAccountId 默认目标账户 ID（可选）
     * @param includePossibleDuplicates 是否导入疑似重复项
     */
    override suspend fun importBills(uri: Uri, rule: BillParseRule, selectedAccountId: Long?, includePossibleDuplicates: Boolean): BillImportResult = withContext(ioDispatcher) {
        val prepared = planBillImport(readImport(uri, rule, selectedAccountId), database.transactionDao().backupRows().map { it.asExternalModel() }, includePossibleDuplicates)
        val primary = preferences.preferences.first().primaryCurrency
        val accounts = database.accountDao().backupRows().associate { it.accountId to it.asExternalModel() }
        val pending = prepared.transactions.map { transaction ->
            valuation.transaction(transaction, accounts.getValue(transaction.accountId), transaction.toAccountId?.let(accounts::getValue), primary)
        }
        database.withTransaction {
            val plan = planBillImport(pending, database.transactionDao().backupRows().map { it.asExternalModel() }, includePossibleDuplicates)
            val balances = database.accountDao().backupRows().associate { it.accountId to it.currentBalance }.toMutableMap()
            plan.transactions.forEach { transaction ->
                currentCoroutineContext().ensureActive()
                fun add(id: Long, delta: Long) { balances[id] = Math.addExact(balances.getValue(id), delta) }
                check(database.accountDao().getAccountByIdOnce(transaction.accountId) != null) { "导入账户已删除" }
                check(database.categoryDao().getCategoryByIdOnce(transaction.categoryId) != null) { "导入分类已删除" }
                if (transaction.type == TransactionType.INCOME) add(transaction.accountId, transaction.amount)
                else add(transaction.accountId, -transaction.amount)
                transaction.toAccountId?.let { id ->
                    check(database.accountDao().getAccountByIdOnce(id) != null) { "转入账户已删除" }
                    add(id, checkNotNull(transaction.toAmount))
                }
                database.transactionDao().insertTransaction(transaction.asEntity())
            }
            balances.forEach { (id, balance) ->
                val account = database.accountDao().getAccountByIdOnce(id)
                if (account != null && account.currentBalance != balance) database.accountDao().updateAccount(account.copy(currentBalance = balance, baseBalance = null))
            }
            BillImportResult(plan.transactions.size, prepared.duplicates + plan.duplicates, if (includePossibleDuplicates) 0 else prepared.possibleDuplicates + plan.possibleDuplicates)
        }
    }

    /**
     * 导出账单与/或设置的完整快照，包含图片 Base64 编码、汇率、偏好设置等，写入指定 URI。
     *
     * @param uri 备份目标 URI
     * @param includeBills 是否包含账单数据
     * @param includeSettings 是否包含偏好与外观设置
     */
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
            BackupAppearance(target, appearance.backgroundFile?.let {
                Base64.encodeToString(File(context.filesDir, it).inputStream().use { input -> input.readBytesLimited(8 * 1024 * 1024) }, Base64.NO_WRAP)
            })
        } else emptyList()
        fun pack(template: CardTemplate) = template.copy(coverUri = packImage(template.coverUri))
        val packed = snapshot.copy(
            bills = snapshot.bills?.copy(templates = snapshot.bills.templates.map(::pack)),
            settings = snapshot.settings?.copy(templates = snapshot.settings.templates.map(::pack), appearances = appearanceData,
                preferences = preferences.preferences.first(), rates = database.exchangeRateDao().rows().map { BackupExchangeRate(it.date, it.currency, it.perUsd, it.manual, it.updatedAtMillis) }),
        )
        LedgerBackupValidator.validate(packed)
        val bytes = json.encodeToString(packed).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BACKUP_BYTES) { "备份文件超过 128 MB" }
        write(uri, bytes)
    }

    /**
     * 从指定 URI 恢复备份数据。
     *
     * 解码 Base64 图片资源、重建数据库表数据、恢复卡片外观和通用偏好，
     * 并在发生异常时提供外观和偏好设置的回滚机制。
     *
     * @param uri 备份文件源 URI
     */
    override suspend fun restore(uri: Uri): Unit = withContext(ioDispatcher) {
        val backup = json.decodeFromString<LedgerBackup>(read(uri, MAX_BACKUP_BYTES).toString(Charsets.UTF_8))
        LedgerBackupValidator.validate(backup)
        val unpackedImages = mutableMapOf<String, String>()
        fun unpack(template: CardTemplate): CardTemplate {
            val normalized = BuiltInCardFace.byId(template.id)?.asTemplate(template) ?: template
            return normalized.copy(coverUri = unpackedImages.getOrPut(normalized.coverUri) { unpackImage(normalized.coverUri) })
        }
        val bills = backup.bills?.let { it.copy(templates = it.templates.map(::unpack)) }
        val settings = backup.settings?.let { it.copy(templates = it.templates.map(::unpack)) }
        // Keep rollback copies because applying a background removes its previous image.
        val prepared = mutableMapOf<SummaryCardTarget, String?>()
        val previous = mutableMapOf<SummaryCardTarget, String?>()
        var appearanceChanged = false
        var preferencesChanged = false
        val previousPreferences = preferences.preferences.first()
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
                previous[appearance.target] = old.backgroundFile?.let { filename ->
                    appearances.prepareBackground(Uri.fromFile(File(context.filesDir, filename)), appearance.target)
                }
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
                    database.cardTemplateDao().ensureBuiltInCardFaces()
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
                        (existing - restoredIds).filter { BuiltInCardFace.byId(it) == null }
                            .forEach { database.cardTemplateDao().softDeleteTemplate(it) }
                        settings.templates.forEach { template ->
                            if (template.id in existing) database.cardTemplateDao().updateTemplate(template.asEntity())
                            else database.cardTemplateDao().insertTemplate(template.asEntity())
                        }
                        database.cardTemplateDao().ensureBuiltInCardFaces()
                    }
                    settings.rules.forEach { rule ->
                        val restored = if (bills == null) rule.copy(defaultAccountId = null, defaultCategoryId = null) else rule
                        database.parseRuleDao().insertRule(restored.asEntity())
                    }
                }
                if (settings != null) {
                    database.exchangeRateDao().clear()
                    database.exchangeRateDao().insert(settings.rates.map { cn.turingzy.ledger.data.database.entity.ExchangeRateEntity(it.date, it.currency, it.perUsd, it.manual, it.updatedAtMillis) })
                    settings.preferences?.let { preferencesChanged = true; preferences.save(it) }
                }
                settings?.appearances?.forEach { appearance ->
                    appearanceChanged = true
                    val image = prepared[appearance.target]
                    if (image != null) appearances.applyPreparedBackground(image, appearance.target) else appearances.resetBackground(appearance.target)
                }
            }
        } catch (error: Exception) {
            if (preferencesChanged) withContext(NonCancellable) {
                try { preferences.save(previousPreferences) }
                catch (rollback: Exception) {
                    if (rollback is kotlinx.coroutines.CancellationException) throw rollback
                    timber.log.Timber.e(rollback, "Unable to roll back general settings")
                    error.addSuppressed(rollback)
                }
            }
            if (appearanceChanged) withContext(NonCancellable) {
                previous.forEach { (target, image) ->
                    try {
                        if (image != null) appearances.applyPreparedBackground(image, target) else appearances.resetBackground(target)
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
                exchangeRates.invalidate()
                (prepared.values + previous.values).filterNotNull().forEach { appearances.discardPreparedBackground(it) }
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
