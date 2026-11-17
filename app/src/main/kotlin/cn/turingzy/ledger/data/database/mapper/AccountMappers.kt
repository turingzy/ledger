package cn.turingzy.ledger.data.database.mapper

import cn.turingzy.ledger.data.database.entity.AccountEntity
import cn.turingzy.ledger.data.database.entity.CardTemplateEntity
import cn.turingzy.ledger.data.database.entity.PopulatedAccount
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.CardTemplate

fun CardTemplateEntity.asExternalModel(): CardTemplate = CardTemplate(
    id = templateId,
    name = name,
    coverUri = coverUri,
    textColor = textColor,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

fun CardTemplate.asEntity(): CardTemplateEntity = CardTemplateEntity(
    templateId = id,
    name = name,
    coverUri = coverUri,
    textColor = textColor,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

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

fun PopulatedAccount.asExternalModel(): AccountWithTemplate = AccountWithTemplate(
    account = account.asExternalModel(),
    template = template?.asExternalModel(),
)
