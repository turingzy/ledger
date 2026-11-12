package cn.turingzy.ledger.database.mapper

import cn.turingzy.ledger.database.model.AccountEntity
import cn.turingzy.ledger.database.model.BackupConfigEntity
import cn.turingzy.ledger.database.model.CardTemplateEntity
import cn.turingzy.ledger.database.model.CategoryEntity
import cn.turingzy.ledger.database.model.IconEntity
import cn.turingzy.ledger.database.model.ParseRuleEntity
import cn.turingzy.ledger.database.model.PopulatedAccount
import cn.turingzy.ledger.database.model.PopulatedCategory
import cn.turingzy.ledger.database.model.PopulatedTransaction
import cn.turingzy.ledger.database.model.TransactionEntity
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.BackupConfig
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.ParseRule
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail

fun IconEntity.asExternalModel(): Icon = Icon(
    id = iconId,
    keyName = keyName,
    displayName = displayName,
    iconUri = iconUri,
    isSystem = isSystem,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
)

fun Icon.asEntity(): IconEntity = IconEntity(
    iconId = id,
    keyName = keyName,
    displayName = displayName,
    iconUri = iconUri,
    isSystem = isSystem,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
)

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

fun CategoryEntity.asExternalModel(): Category = Category(
    id = categoryId,
    name = name,
    type = type,
    iconId = iconId,
    sortOrder = sortOrder,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

fun Category.asEntity(): CategoryEntity = CategoryEntity(
    categoryId = id,
    name = name,
    type = type,
    iconId = iconId,
    sortOrder = sortOrder,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

fun PopulatedCategory.asExternalModel(): CategoryWithIcon = CategoryWithIcon(
    category = category.asExternalModel(),
    icon = icon.asExternalModel(),
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
)

fun PopulatedAccount.asExternalModel(): AccountWithTemplate = AccountWithTemplate(
    account = account.asExternalModel(),
    template = template?.asExternalModel(),
)

fun TransactionEntity.asExternalModel(): Transaction = Transaction(
    id = transactionId,
    type = type,
    amount = amount,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    transactionTime = transactionTime,
    note = note,
    isDeleted = isDeleted,
)

fun Transaction.asEntity(): TransactionEntity = TransactionEntity(
    transactionId = id,
    type = type,
    amount = amount,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    transactionTime = transactionTime,
    note = note,
    isDeleted = isDeleted,
)

fun PopulatedTransaction.asExternalModel(): TransactionDetail = TransactionDetail(
    transaction = transaction.asExternalModel(),
    account = account.asExternalModel(),
    toAccount = toAccount?.asExternalModel(),
    category = categoryWithIcon.asExternalModel(),
)

fun ParseRuleEntity.asExternalModel(): ParseRule = ParseRule(
    id = ruleId,
    name = name,
    startRow = startRow,
    mappingConfig = mappingConfig,
    defaultAccountId = defaultAccountId,
    defaultCategoryId = defaultCategoryId,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

fun ParseRule.asEntity(): ParseRuleEntity = ParseRuleEntity(
    ruleId = id,
    name = name,
    startRow = startRow,
    mappingConfig = mappingConfig,
    defaultAccountId = defaultAccountId,
    defaultCategoryId = defaultCategoryId,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

fun BackupConfigEntity.asExternalModel(): BackupConfig = BackupConfig(
    id = configId,
    name = name,
    providerType = providerType,
    endpointUrl = endpointUrl,
    accountIdentity = accountIdentity,
    encryptedSecret = encryptedSecret,
    remoteDirectory = remoteDirectory,
    isAutoBackup = isAutoBackup,
    lastBackupAt = lastBackupAt,
)

fun BackupConfig.asEntity(): BackupConfigEntity = BackupConfigEntity(
    configId = id,
    name = name,
    providerType = providerType,
    endpointUrl = endpointUrl,
    accountIdentity = accountIdentity,
    encryptedSecret = encryptedSecret,
    remoteDirectory = remoteDirectory,
    isAutoBackup = isAutoBackup,
    lastBackupAt = lastBackupAt,
)

