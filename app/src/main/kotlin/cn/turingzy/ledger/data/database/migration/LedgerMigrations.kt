package cn.turingzy.ledger.data.database.migration

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

internal val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tb_transaction ADD COLUMN import_key TEXT")
    }
}

internal val Migration3To4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("base_amount INTEGER", "base_currency TEXT", "base_rate TEXT", "base_rate_date TEXT", "to_amount INTEGER", "transfer_rate TEXT", "currency_snapshots TEXT NOT NULL DEFAULT '[]'").forEach {
            db.execSQL("ALTER TABLE tb_transaction ADD COLUMN $it")
        }
        listOf("base_balance INTEGER", "base_currency TEXT", "base_rate TEXT", "base_rate_date TEXT").forEach {
            db.execSQL("ALTER TABLE tb_account ADD COLUMN $it")
        }
        db.execSQL("UPDATE tb_transaction SET to_amount = amount WHERE type = 'TRANSFER'")
        db.execSQL("CREATE TABLE IF NOT EXISTS tb_exchange_rate (date TEXT NOT NULL, currency TEXT NOT NULL, perUsd TEXT NOT NULL, manual INTEGER NOT NULL, PRIMARY KEY(date, currency))")
    }
}

/** Leaves historical rates without a timestamp until they are fetched or edited. */
internal val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tb_exchange_rate ADD COLUMN updated_at_millis INTEGER")
    }
}
