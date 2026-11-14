package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.*
import kotlinx.serialization.json.Json

object LedgerBackupValidator {
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
            require(bills.transactions.all {
                it.amount > 0 && it.accountId in accounts && it.categoryId in categories &&
                    (it.type != TransactionType.TRANSFER || (it.toAccountId in accounts && it.toAccountId != it.accountId)) &&
                    (it.toAccountId == null || it.toAccountId in accounts)
            }) { "账单金额或引用无效" }

        }
        backup.settings?.let { settings ->
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

    private fun ids(values: List<Long>): Set<Long> {
        require(values.all { it != 0L } && values.distinct().size == values.size) { "备份包含无效或重复编号" }
        return values.toSet()
    }
}
