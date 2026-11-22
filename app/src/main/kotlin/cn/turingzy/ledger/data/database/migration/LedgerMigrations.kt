package cn.turingzy.ledger.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库版本 1 -> 2 迁移：
 *
 * 修正内置转账分类（ID 为 11）的分类类型，从 EXPENSE 更新为 TRANSFER。
 */
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

/**
 * 数据库版本 2 -> 3 迁移：
 *
 * 在 `tb_transaction` 表中新增 `import_key` 字段，用于账单导入时的幂等去重校验。
 */
internal val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tb_transaction ADD COLUMN import_key TEXT")
    }
}

/**
 * 数据库版本 3 -> 4 迁移：
 *
 * 1. 在 `tb_transaction` 中增加基准货币折算金额、转账汇率及快照列。
 * 2. 在 `tb_account` 中增加基准货币估值缓存列。
 * 3. 历史转账记录默认设置 `to_amount = amount`。
 * 4. 新建汇率表 `tb_exchange_rate`。
 */
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

/**
 * 数据库版本 4 -> 5 迁移：
 *
 * 在 `tb_exchange_rate` 汇率表中增加 `updated_at_millis` 字段，用于记录汇率拉取或修改时间戳。
 */
internal val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tb_exchange_rate ADD COLUMN updated_at_millis INTEGER")
    }
}
