package cn.turingzy.ledger.data.database.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import cn.turingzy.ledger.data.database.entity.PopulatedTransaction
import cn.turingzy.ledger.data.database.entity.TransactionEntity
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail

/**
 * 将交易实体 [TransactionEntity] 转换为业务领域模型 [Transaction]。
 *
 * 其中 [TransactionEntity.currencySnapshots] 会通过 JSON 反序列化为货币快照列表。
 */
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
    importKey = importKey,
    baseAmount = baseAmount,
    baseCurrency = baseCurrency,
    baseRate = baseRate,
    baseRateDate = baseRateDate,
    toAmount = toAmount,
    transferRate = transferRate,
    currencySnapshots = Json.decodeFromString(currencySnapshots),
)

/**
 * 将业务领域模型 [Transaction] 转换为数据库实体 [TransactionEntity]。
 *
 * 其中 [Transaction.currencySnapshots] 会被 JSON 序列化为字符串保存。
 */
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
    importKey = importKey,
    baseAmount = baseAmount,
    baseCurrency = baseCurrency,
    baseRate = baseRate,
    baseRateDate = baseRateDate,
    toAmount = toAmount,
    transferRate = transferRate,
    currencySnapshots = Json.encodeToString(currencySnapshots),
)

/**
 * 将包含源账户、目标账户和带图标分类的聚合交易实体 [PopulatedTransaction] 转换为领域聚合模型 [TransactionDetail]。
 */
fun PopulatedTransaction.asExternalModel(): TransactionDetail = TransactionDetail(
    transaction = transaction.asExternalModel(),
    account = account.asExternalModel(),
    toAccount = toAccount?.asExternalModel(),
    category = categoryWithIcon.asExternalModel(),
)
