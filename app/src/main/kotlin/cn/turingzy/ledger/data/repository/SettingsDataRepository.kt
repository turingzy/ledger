package cn.turingzy.ledger.data.repository

import android.net.Uri
import cn.turingzy.ledger.model.BillParseRule
import cn.turingzy.ledger.model.CardTemplate

interface SettingsDataRepository {
    suspend fun ensureDefaultRules()
    suspend fun saveTemplate(template: CardTemplate): Long
    suspend fun copyImage(uri: Uri): String
    suspend fun exportBills(uri: Uri, excel: Boolean, start: Long, end: Long): Int
    suspend fun importBills(uri: Uri, rule: BillParseRule, selectedAccountId: Long?): Int
    suspend fun backup(uri: Uri, includeBills: Boolean, includeSettings: Boolean)
    suspend fun restore(uri: Uri)
}
