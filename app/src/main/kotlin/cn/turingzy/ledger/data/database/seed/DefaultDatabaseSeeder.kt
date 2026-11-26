package cn.turingzy.ledger.data.database.seed

import androidx.room.withTransaction
import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.dao.CardTemplateDao
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.model.account.BuiltInCardFace
import cn.turingzy.ledger.data.database.entity.CategoryEntity
import cn.turingzy.ledger.data.database.entity.IconEntity
import cn.turingzy.ledger.model.category.CategoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 数据库初始种子数据填充器。
 *
 * 在初次创建数据库或初始化时，填充默认的系统图标、卡片模板、基础收支与转账分类以及预置账单解析规则。
 */
object DefaultDatabaseSeeder {

    /**
     * 异步填充数据库初始种子数据。
     *
     * @param database 数据库实例
     * @param scope 协程作用域
     */
    fun seedInitialData(database: LedgerDatabase, scope: CoroutineScope) {
        scope.launch {
            val defaultIcons = listOf(
                IconEntity(iconId = 1, keyName = "ic_food", displayName = "餐饮", iconUri = "res://drawable/ic_food", isSystem = true, sortOrder = 1),
                IconEntity(iconId = 2, keyName = "ic_shopping", displayName = "购物", iconUri = "res://drawable/ic_shopping", isSystem = true, sortOrder = 2),
                IconEntity(iconId = 3, keyName = "ic_transport", displayName = "交通", iconUri = "res://drawable/ic_transport", isSystem = true, sortOrder = 3),
                IconEntity(iconId = 4, keyName = "ic_entertainment", displayName = "娱乐", iconUri = "res://drawable/ic_entertainment", isSystem = true, sortOrder = 4),
                IconEntity(iconId = 5, keyName = "ic_housing", displayName = "居住", iconUri = "res://drawable/ic_housing", isSystem = true, sortOrder = 5),
                IconEntity(iconId = 6, keyName = "ic_medical", displayName = "医疗", iconUri = "res://drawable/ic_medical", isSystem = true, sortOrder = 6),
                IconEntity(iconId = 7, keyName = "ic_salary", displayName = "工资", iconUri = "res://drawable/ic_salary", isSystem = true, sortOrder = 7),
                IconEntity(iconId = 8, keyName = "ic_investment", displayName = "理财", iconUri = "res://drawable/ic_investment", isSystem = true, sortOrder = 8),
                IconEntity(iconId = 9, keyName = "ic_transfer", displayName = "转账", iconUri = "res://drawable/ic_transfer", isSystem = true, sortOrder = 9),
                IconEntity(iconId = 10, keyName = "ic_other", displayName = "其他", iconUri = "res://drawable/ic_other", isSystem = true, sortOrder = 10),
            )
            val defaultCategories = listOf(
                CategoryEntity(categoryId = 1, name = "餐饮", type = CategoryType.EXPENSE, iconId = 1, sortOrder = 1, isSystem = true),
                CategoryEntity(categoryId = 2, name = "购物", type = CategoryType.EXPENSE, iconId = 2, sortOrder = 2, isSystem = true),
                CategoryEntity(categoryId = 3, name = "交通", type = CategoryType.EXPENSE, iconId = 3, sortOrder = 3, isSystem = true),
                CategoryEntity(categoryId = 4, name = "娱乐", type = CategoryType.EXPENSE, iconId = 4, sortOrder = 4, isSystem = true),
                CategoryEntity(categoryId = 5, name = "居住", type = CategoryType.EXPENSE, iconId = 5, sortOrder = 5, isSystem = true),
                CategoryEntity(categoryId = 6, name = "医疗", type = CategoryType.EXPENSE, iconId = 6, sortOrder = 6, isSystem = true),
                CategoryEntity(categoryId = 7, name = "其他支出", type = CategoryType.EXPENSE, iconId = 10, sortOrder = 7, isSystem = true),
                CategoryEntity(categoryId = 8, name = "工资", type = CategoryType.INCOME, iconId = 7, sortOrder = 1, isSystem = true),
                CategoryEntity(categoryId = 9, name = "理财收益", type = CategoryType.INCOME, iconId = 8, sortOrder = 2, isSystem = true),
                CategoryEntity(categoryId = 10, name = "其他收入", type = CategoryType.INCOME, iconId = 10, sortOrder = 3, isSystem = true),
                CategoryEntity(categoryId = 11, name = "转账", type = CategoryType.TRANSFER, iconId = 9, sortOrder = 99, isSystem = true),
            )
            database.withTransaction {
                database.iconDao().insertIcons(defaultIcons)
                database.categoryDao().insertCategories(defaultCategories)
                database.openHelper.writableDatabase.seedExpandedSystemCategories()
            }

            database.cardTemplateDao().ensureBuiltInCardFaces()

            database.parseRuleDao().insertRules(cn.turingzy.ledger.model.transfer.BillParseRule.defaults().map { rule -> cn.turingzy.ledger.data.database.entity.ParseRuleEntity(
                ruleId = rule.id, name = rule.name, startRow = rule.startRow.toInt(),
                mappingConfig = kotlinx.serialization.json.Json.encodeToString(cn.turingzy.ledger.model.transfer.BillParseRule.serializer(), rule), isSystem = true,
            ) })
        }
    }
}

/**
 * 确保内置的卡面预设模板全部写入数据库（如果已存在则忽略）。
 */
internal suspend fun CardTemplateDao.ensureBuiltInCardFaces() {
    insertTemplates(BuiltInCardFace.entries.map { it.asTemplate().asEntity() })
}
