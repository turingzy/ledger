package cn.turingzy.ledger.data.database.seed

import androidx.sqlite.db.SupportSQLiteDatabase
import cn.turingzy.ledger.model.category.CategoryType

private val commonExpenseCategories = listOf(
    "买菜" to "grocery",
    "咖啡茶饮" to "local_cafe",
    "日用品" to "shopping_cart",
    "房租" to "key",
    "水电燃气" to "bolt",
    "通信网络" to "wifi",
    "教育培训" to "school",
    "运动健身" to "fitness_center",
    "宠物" to "pets",
    "旅游" to "flight_takeoff",
    "汽车养护" to "build",
    "加油充电" to "local_gas_station",
    "服饰鞋包" to "checkroom",
    "美妆个护" to "spa",
    "保险" to "verified_user",
    "送礼" to "redeem",
)

private val commonIncomeCategories = listOf(
    "绩效奖金" to "military_tech",
    "年终奖" to "celebration",
    "加班费" to "more_time",
    "兼职" to "work",
    "生意收入" to "storefront",
    "补贴津贴" to "volunteer_activism",
    "报销" to "receipt_long",
    "退款" to "assignment_return",
    "消费返现" to "percent",
    "红包礼金" to "mail",
    "租金收入" to "apartment",
    "银行利息" to "savings",
    "股息分红" to "pie_chart",
    "二手转卖" to "sell",
    "稿费" to "edit_note",
    "养老金" to "elderly",
)

/** 在建库和升级时添加常用分类，保留现有分类 ID、自定义同名分类和软删除记录。 */
internal fun SupportSQLiteDatabase.seedExpandedSystemCategories() {
    seedCommonCategories(CategoryType.EXPENSE, commonExpenseCategories, firstCategoryOrder = 8, firstIconOrder = 11)
    seedCommonCategories(
        CategoryType.INCOME, commonIncomeCategories,
        firstCategoryOrder = 4, firstIconOrder = 11 + commonExpenseCategories.size,
    )

    insertSystemCategoryIcon("其他收入", "add_circle", 11 + commonExpenseCategories.size + commonIncomeCategories.size)
    execSQL(
        """
        UPDATE tb_category SET icon_id = COALESCE(
            (SELECT icon_id FROM tb_icon WHERE key_name = 'ic_category_add_circle' AND is_deleted = 0), icon_id
        )
        WHERE category_id = 10 AND type = 'INCOME' AND name = '其他收入' AND is_system = 1 AND is_deleted = 0
          AND icon_id = (SELECT icon_id FROM tb_icon WHERE key_name = 'ic_other')
        """.trimIndent(),
    )
}

/** 自动分配主键，按同类型的名称或已绑定的内置图标判断分类是否存在。 */
private fun SupportSQLiteDatabase.seedCommonCategories(
    type: CategoryType,
    categories: List<Pair<String, String>>,
    firstCategoryOrder: Int,
    firstIconOrder: Int,
) {
    categories.forEachIndexed { index, (name, symbol) ->
        val iconKey = insertSystemCategoryIcon(name, symbol, firstIconOrder + index)
        execSQL(
            """
            INSERT INTO tb_category (name, type, icon_id, sort_order, is_system)
            SELECT ?, ?, icon_id, ?, 1 FROM tb_icon
            WHERE key_name = ? AND is_deleted = 0 AND NOT EXISTS (
                SELECT 1 FROM tb_category
                WHERE type = ? AND (name = ? OR (is_system = 1 AND icon_id = tb_icon.icon_id))
            )
            """.trimIndent(),
            arrayOf<Any>(name, type.name, firstCategoryOrder + index, iconKey, type.name, name),
        )
    }
}

/** 按稳定键查找系统图标，避免与用户或备份中的图标主键冲突。 */
private fun SupportSQLiteDatabase.insertSystemCategoryIcon(name: String, symbol: String, order: Int): String {
    val key = "ic_category_$symbol"
    execSQL(
        "INSERT OR IGNORE INTO tb_icon (key_name, display_name, icon_uri, is_system, sort_order) VALUES (?, ?, ?, 1, ?)",
        arrayOf<Any>(key, name, "material-symbols://$symbol", order),
    )
    return key
}
