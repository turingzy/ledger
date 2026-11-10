package cn.turingzy.ledger.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE tb_category SET type = 'TRANSFER'
            WHERE category_id = 11 AND is_system = 1 AND type = 'EXPENSE' AND is_deleted = 0
            """.trimIndent(),
        )
    }
}
