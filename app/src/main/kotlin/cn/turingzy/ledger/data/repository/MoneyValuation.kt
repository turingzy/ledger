package cn.turingzy.ledger.data.repository

import androidx.room.withTransaction
import cn.turingzy.ledger.database.LedgerDatabase
import cn.turingzy.ledger.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import timber.log.Timber

@Singleton
class MoneyValuation @Inject constructor(private val database: LedgerDatabase, private val rates: ExchangeRateRepository) {
    suspend fun transaction(value: Transaction, source: Account, target: Account?, primary: String): Transaction {
        val date = Instant.ofEpochMilli(value.transactionTime).atZone(ZoneId.systemDefault()).toLocalDate()
        val snapshots = value.currencySnapshots.toMutableList()
        if (value.baseCurrency != null && value.baseAmount != null && snapshots.none { it.currency == value.baseCurrency }) {
            val rate = value.baseRate ?: java.math.BigDecimal.valueOf(value.baseAmount).divide(java.math.BigDecimal.valueOf(value.amount), 18, java.math.RoundingMode.HALF_UP).toPlainString()
            snapshots += CurrencySnapshot(value.baseCurrency, value.baseAmount, rate, value.baseRateDate ?: date.toString())
        }
        val snapshot = snapshots.firstOrNull { it.currency == primary } ?: rates.quote(source.currency, primary, date).let { quote ->
            CurrencySnapshot(primary, convertCents(value.amount, quote.rate), quote.rate, quote.date).also { snapshots += it }
        }
        val transferred = if (value.type != TransactionType.TRANSFER) null
            else value.toAmount ?: rates.quote(source.currency, requireNotNull(target).currency, date).let { convertCents(value.amount, it.rate) }
        require(transferred == null || transferred > 0L && (target?.currency != source.currency || transferred == value.amount)) { "转入金额必须大于零" }
        return value.copy(
            baseCurrency = primary,
            baseAmount = snapshot.amount,
            baseRate = snapshot.rate,
            baseRateDate = snapshot.date,
            currencySnapshots = snapshots,
            toAmount = transferred,
            transferRate = if (transferred != null) java.math.BigDecimal.valueOf(transferred)
                .divide(java.math.BigDecimal.valueOf(value.amount), 18, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() else null,
        )
    }

    suspend fun transactions(entries: List<TransactionDetail>, primary: String): List<TransactionDetail> {
        val pending = entries.filter { it.transaction.baseCurrency != primary || it.transaction.baseAmount == null || it.transaction.currencySnapshots.none { snapshot -> snapshot.currency == primary } ||
            it.transaction.type == TransactionType.TRANSFER && it.transaction.toAmount == null }
        if (pending.isEmpty()) return entries
        val zone = ZoneId.systemDefault()
        if (pending.any { (it.account.currency != primary && it.transaction.currencySnapshots.none { snapshot -> snapshot.currency == primary }) || it.transaction.type == TransactionType.TRANSFER && it.toAccount?.currency != it.account.currency }) rates.prepare(pending.map { Instant.ofEpochMilli(it.transaction.transactionTime).atZone(zone).toLocalDate() }.toSet(),
            (pending.map { it.account.currency } + pending.mapNotNull { it.toAccount?.currency } + primary).toSet())
        val pendingIds = pending.map { it.transaction.id }.toSet()
        val changes = mutableListOf<Transaction>()
        var failure: Exception? = null
        val valued = entries.map { entry ->
            if (entry.transaction.id !in pendingIds) return@map entry
            try {
                val updated = transaction(entry.transaction, entry.account, entry.toAccount, primary)
                changes += updated
                entry.copy(transaction = updated)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { failure = error; entry }
        }
        failure?.let { Timber.w(it, "Some transactions need an exchange rate") }
        if (changes.isNotEmpty()) database.withTransaction {
            changes.forEach { updated ->
                val current = database.transactionDao().getTransactionByIdOnce(updated.id) ?: return@forEach
                if (current.amount != updated.amount || current.transactionTime != updated.transactionTime || current.accountId != updated.accountId ||
                    current.toAccountId != updated.toAccountId || current.type != updated.type) return@forEach
                val existing = Json.decodeFromString<List<CurrencySnapshot>>(current.currencySnapshots)
                val merged = (existing + updated.currencySnapshots).distinctBy { it.currency }
                val snapshot = merged.first { it.currency == primary }
                database.transactionDao().updateValuation(updated.id, updated.amount, updated.transactionTime,
                    snapshot.amount, primary, snapshot.rate, snapshot.date, updated.toAmount, updated.transferRate, Json.encodeToString(merged))
            }
        }
        return valued
    }

    suspend fun accounts(entries: List<AccountWithTemplate>, primary: String): List<AccountWithTemplate> {
        val today = LocalDate.now()
        if (entries.any { it.account.currentBalance != 0L && it.account.currency != primary }) rates.prepare(setOf(today), (entries.filter { it.account.currentBalance != 0L }.map { it.account.currency } + primary).toSet())
        return entries.map { entry ->
            val account = entry.account
            try {
                if (account.currentBalance == 0L) {
                    val zero = account.copy(baseBalance = 0L, baseCurrency = primary,
                        baseRate = if (account.currency == primary) "1" else null, baseRateDate = today.toString())
                    if (zero != account) database.accountDao().updateValuation(account.id, 0L, 0L, primary, zero.baseRate, today.toString())
                    return@map entry.copy(account = zero)
                }
                val quote = rates.quote(account.currency, primary, today)
                val balance = convertCents(account.currentBalance, quote.rate)
                val valued = account.copy(baseBalance = balance, baseCurrency = primary, baseRate = quote.rate, baseRateDate = quote.date)
                if (valued != account) database.accountDao().updateValuation(account.id, account.currentBalance, balance, primary, quote.rate, quote.date)
                entry.copy(account = valued)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                Timber.w(error, "Account %s needs an exchange rate", account.id)
                entry.copy(account = account.copy(baseBalance = null, baseCurrency = primary))
            }
        }
    }
}
