package cn.turingzy.ledger.model.backup

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transfer.ParseRule
import kotlinx.serialization.Serializable

@Serializable
data class LedgerBackup(
    val format: String = "cn.turingzy.ledger.backup",
    val version: Int = 1,
    val createdAt: Long = 0L,
    val bills: LedgerBillSnapshot? = null,
    val settings: LedgerSettingsSnapshot? = null,
)

@Serializable
data class LedgerBillSnapshot(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val icons: List<Icon>,
    val templates: List<CardTemplate>,
)

@Serializable
data class LedgerSettingsSnapshot(
    val rules: List<ParseRule>,
    val templates: List<CardTemplate>,
    val appearances: List<BackupAppearance> = emptyList(),
    val preferences: AppPreferences? = null,
    val rates: List<BackupExchangeRate> = emptyList(),
)

@Serializable
data class BackupAppearance(val target: SummaryCardTarget, val transparency: Int, val image: String? = null)

@Serializable
data class BackupExchangeRate(val date: String, val currency: String, val perUsd: String, val manual: Boolean = false)
