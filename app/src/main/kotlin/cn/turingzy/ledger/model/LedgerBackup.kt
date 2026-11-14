package cn.turingzy.ledger.model

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
)

@Serializable
data class BackupAppearance(val target: SummaryCardTarget, val transparency: Int, val image: String? = null)
