package cn.turingzy.ledger.database.util

import cn.turingzy.ledger.database.LedgerDatabase
import cn.turingzy.ledger.database.model.AccountEntity
import cn.turingzy.ledger.database.model.CardTemplateEntity
import cn.turingzy.ledger.database.model.CategoryEntity
import cn.turingzy.ledger.database.model.IconEntity
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CategoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object DefaultDatabaseSeeder {

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
            database.iconDao().insertIcons(defaultIcons)

            val defaultTemplates = listOf(
                CardTemplateEntity(templateId = 1, name = "经典深蓝", coverUri = "res://drawable/card_classic_blue", textColor = "#FFFFFF", isSystem = true),
                CardTemplateEntity(templateId = 2, name = "商务黑金", coverUri = "res://drawable/card_black_gold", textColor = "#D4AF37", isSystem = true),
                CardTemplateEntity(templateId = 3, name = "活力薄荷", coverUri = "res://drawable/card_mint_green", textColor = "#FFFFFF", isSystem = true),
            )
            database.cardTemplateDao().insertTemplates(defaultTemplates)

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
            database.categoryDao().insertCategories(defaultCategories)

            val defaultAccounts = listOf(
                AccountEntity(accountId = 1, name = "现金", type = AccountType.CASH, templateId = 1, sortOrder = 1),
                AccountEntity(accountId = 2, name = "微信零钱", type = AccountType.WALLET, templateId = 3, sortOrder = 2),
                AccountEntity(accountId = 3, name = "支付宝", type = AccountType.WALLET, templateId = 1, sortOrder = 3),
            )
            database.accountDao().insertAccounts(defaultAccounts)
        }
    }
}

