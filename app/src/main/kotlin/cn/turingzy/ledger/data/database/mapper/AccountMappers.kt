package cn.turingzy.ledger.data.database.mapper

import cn.turingzy.ledger.data.database.entity.AccountEntity
import cn.turingzy.ledger.data.database.entity.CardTemplateEntity
import cn.turingzy.ledger.data.database.entity.PopulatedAccount
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.CardTemplate

/**
 * 将卡片模板实体 [CardTemplateEntity] 转换为业务领域模型 [CardTemplate]。
 */
fun CardTemplateEntity.asExternalModel(): CardTemplate = CardTemplate(
    id = templateId,
    name = name,
    coverUri = coverUri,
    textColor = textColor,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

/**
 * 将业务领域模型 [CardTemplate] 转换为数据库实体 [CardTemplateEntity]。
 */
fun CardTemplate.asEntity(): CardTemplateEntity = CardTemplateEntity(
    templateId = id,
    name = name,
    coverUri = coverUri,
    textColor = textColor,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

/**
 * 将账户实体 [AccountEntity] 转换为业务领域模型 [Account]。
 */
fun AccountEntity.asExternalModel(): Account = Account(
    id = accountId,
    name = name,
    type = type,
    initialBalance = initialBalance,
    currentBalance = currentBalance,
    currency = currency,
    templateId = templateId,
    cardNumber = cardNumber,
    includeInTotal = includeInTotal,
    isDeleted = isDeleted,
    baseBalance = baseBalance,
    baseCurrency = baseCurrency,
    baseRate = baseRate,
    baseRateDate = baseRateDate,
)

/**
 * 将业务领域模型 [Account] 转换为数据库实体 [AccountEntity]。
 */
fun Account.asEntity(): AccountEntity = AccountEntity(
    accountId = id,
    name = name,
    type = type,
    initialBalance = initialBalance,
    currentBalance = currentBalance,
    currency = currency,
    templateId = templateId,
    cardNumber = cardNumber,
    includeInTotal = includeInTotal,
    isDeleted = isDeleted,
    baseBalance = baseBalance,
    baseCurrency = baseCurrency,
    baseRate = baseRate,
    baseRateDate = baseRateDate,
)

/**
 * 将包含关联模板的聚合账户实体 [PopulatedAccount] 转换为外部领域聚合模型 [AccountWithTemplate]。
 */
fun PopulatedAccount.asExternalModel(): AccountWithTemplate = AccountWithTemplate(
    account = account.asExternalModel(),
    template = template?.asExternalModel(),
)
