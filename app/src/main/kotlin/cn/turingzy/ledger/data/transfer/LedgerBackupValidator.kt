package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.transfer.BillParseRule
import cn.turingzy.ledger.model.backup.LedgerBackup
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.model.currency.convertCents
import cn.turingzy.ledger.model.settings.supportedCurrencyCodes
import kotlinx.serialization.json.Json

/**
 * 账本备份数据校验器。
 *
 * 负责在导入或恢复备份时，严格核验备份格式标识、数据版本、实体关联完整性（账户、分类、图标、模板引用的有效性）
 * 以及金额、汇率快照、偏好配置等业务规则约束。
 */
object LedgerBackupValidator {
    /**
     * 校验账本备份对象的完整性与合法性。
     *
     * 检查项包括：
     * - 备份格式标识（`cn.turingzy.ledger.backup`）与版本号（当前固定为 1）。
     * - 账单数据存在性及其内部实体 ID 的非零与唯一性。
     * - 账户关联卡面模板引用的存在性。
     * - 分类关联图标引用的存在性。
     * - 交易记录中账户、目标转账账户（自转禁止且不能相同）、分类引用的有效性。
     * - 交易货币快照的币种支持、金额非负与汇率换算一致性。
     * - 偏好设置中的主要货币、主题及语言取值有效性。
     * - 汇率历史数据的合法性。
     * - 导入解析规则的有效性及其默认关联账户/分类的外键引用。
     * - 卡片外观透明度取值范围（0..100）及目标唯一性。
     *
     * @param backup 待验证的账本备份数据对象 [LedgerBackup]。
     * @throws IllegalArgumentException 当数据存在结构错误、孤立引用或违反约束条件时抛出。
     */
    fun validate(backup: LedgerBackup) {
        require(backup.format == "cn.turingzy.ledger.backup" && backup.version == 1) { "不支持的备份格式或版本" }
        require(backup.bills != null || backup.settings != null) { "备份中没有数据" }
        backup.bills?.let { bills ->
            val accounts = ids(bills.accounts.map { it.id })
            val categories = ids(bills.categories.map { it.id })
            val icons = ids(bills.icons.map { it.id })
            val templates = ids(bills.templates.map { it.id })
            ids(bills.transactions.map { it.id })
            require(bills.accounts.all { it.templateId == null || it.templateId in templates }) { "账户卡面引用无效" }
            require(bills.categories.all { it.iconId in icons }) { "分类图标引用无效" }
            require(bills.transactions.all { transaction ->
                transaction.currencySnapshots.map { it.currency }.distinct().size == transaction.currencySnapshots.size &&
                    transaction.currencySnapshots.all { snapshot ->
                        snapshot.currency in supportedCurrencyCodes && snapshot.amount >= 0L &&
                            runCatching { java.time.LocalDate.parse(snapshot.date); convertCents(transaction.amount, snapshot.rate) == snapshot.amount }.getOrDefault(false)
                    } && (transaction.toAmount == null || transaction.type == TransactionType.TRANSFER && transaction.toAmount > 0L)
            }) { "账单换算数据无效" }
            require(bills.transactions.all {
                it.amount > 0 && it.accountId in accounts && it.categoryId in categories &&
                    (it.type != TransactionType.TRANSFER || (it.toAccountId in accounts && it.toAccountId != it.accountId)) &&
                    (it.toAccountId == null || it.toAccountId in accounts)
            }) { "账单金额或引用无效" }

        }
        backup.settings?.let { settings ->
            settings.preferences?.let {
                require(it.primaryCurrency in supportedCurrencyCodes && it.theme in setOf("system", "light", "dark") && it.language in setOf("system", "zh", "en")) { "常规无效" }
            }
            require(settings.rates.all { it.currency.matches(Regex("[A-Z]{3}")) && it.perUsd.toBigDecimalOrNull()?.signum() == 1 &&
                runCatching { java.time.LocalDate.parse(it.date) }.isSuccess }) { "备份汇率无效" }
            ids(settings.rules.map { it.id }); ids(settings.templates.map { it.id })
            require(settings.rules.filterNot { it.isDeleted }.all {
                it.name.isNotBlank() && it.startRow > 0 && Json.decodeFromString<BillParseRule>(it.mappingConfig).valid
            }) { "备份包含无效的解析规则" }
            require(settings.appearances.map { it.target }.distinct().size == settings.appearances.size) { "重复的卡片设置" }
            require(settings.appearances.all { it.transparency in 0..100 }) { "透明度设置无效" }
            backup.bills?.let { bills ->
                require(settings.rules.all { rule ->
                    (rule.defaultAccountId == null || bills.accounts.any { it.id == rule.defaultAccountId }) &&
                        (rule.defaultCategoryId == null || bills.categories.any { it.id == rule.defaultCategoryId })
                }) { "解析规则引用无效" }
            }
        }
    }

    /**
     * 校验实体 ID 列表，确保所有 ID 均非零且无重复值，返回唯一的 ID 集合。
     */
    private fun ids(values: List<Long>): Set<Long> {
        require(values.all { it != 0L } && values.distinct().size == values.size) { "备份包含无效或重复编号" }
        return values.toSet()
    }
}
