package cn.turingzy.ledger.data.repository.settings

import android.net.Uri
import cn.turingzy.ledger.model.transfer.BillParseRule
import cn.turingzy.ledger.model.account.CardTemplate

interface SettingsDataRepository {
    suspend fun ensureDefaultRules()
    suspend fun saveTemplate(template: CardTemplate): Long
    suspend fun copyImage(uri: Uri): String
    suspend fun exportBills(uri: Uri, excel: Boolean, start: Long, end: Long): Int
    suspend fun previewImport(uri: Uri, rule: BillParseRule, selectedAccountId: Long?): cn.turingzy.ledger.model.transfer.BillImportResult
    suspend fun importBills(uri: Uri, rule: BillParseRule, selectedAccountId: Long?, includePossibleDuplicates: Boolean = false): cn.turingzy.ledger.model.transfer.BillImportResult
    suspend fun backup(uri: Uri, includeBills: Boolean, includeSettings: Boolean)
    suspend fun restore(uri: Uri)
}
