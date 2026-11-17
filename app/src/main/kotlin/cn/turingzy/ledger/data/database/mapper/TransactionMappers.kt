package cn.turingzy.ledger.data.database.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import cn.turingzy.ledger.data.database.entity.PopulatedTransaction
import cn.turingzy.ledger.data.database.entity.TransactionEntity
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail

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

fun PopulatedTransaction.asExternalModel(): TransactionDetail = TransactionDetail(
    transaction = transaction.asExternalModel(),
    account = account.asExternalModel(),
    toAccount = toAccount?.asExternalModel(),
    category = categoryWithIcon.asExternalModel(),
)
