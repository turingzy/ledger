package cn.turingzy.ledger.data.repository.currency

import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.entity.ExchangeRateEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

data class ExchangeQuote(val rate: String, val date: String)

@Singleton
class ExchangeRateRepository @Inject constructor(private val database: LedgerDatabase, private val source: ExchangeRateSource) {
    private val mutex = Mutex()
    private val attempts = mutableSetOf<String>()
    private var lastFailureAt = 0L
    private val _changes = MutableStateFlow(0L)
    val changes = _changes.asStateFlow()

    suspend fun invalidate() = mutex.withLock { attempts.clear(); lastFailureAt = 0L; _changes.value++ }

    suspend fun prepare(dates: Set<LocalDate>, currencies: Set<String>, refresh: Boolean = false) = mutex.withLock {
        if (!refresh && System.currentTimeMillis() - lastFailureAt < 60_000L) return@withLock
        val quotes = currencies - "USD"
        if (dates.isEmpty() || quotes.isEmpty()) return@withLock
        val existing = database.exchangeRateDao().rows()
        val missing = dates.filter { date ->
            quotes.any { currency -> existing.none { it.currency == currency && it.date <= date.toString() && it.date >= date.minusDays(7).toString() } }
        }
        val today = LocalDate.now()
        val key = "${today}:${dates.min()}:${dates.max()}:${quotes.sorted()}"
        if (!refresh && key in attempts) return@withLock
        if (!refresh && missing.isEmpty() && today !in dates) return@withLock
        attempts += key
        try {
            val start = (missing.minOrNull() ?: dates.min()).minusDays(7)
            val rows = source.fetch(start, minOf(dates.max(), today), currencies)
            val byKey = existing.associateBy { it.date to it.currency }
            val changed = rows.filter { row -> byKey[row.date to row.currency]?.let { !it.manual && it != row } ?: true }
            if (changed.isNotEmpty()) {
                database.exchangeRateDao().insert(changed)
                _changes.value++
            }
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) { lastFailureAt = System.currentTimeMillis(); Timber.w(error, "Unable to refresh exchange rates; using cached values") }
    }

    suspend fun quote(from: String, to: String, date: LocalDate): ExchangeQuote {
        if (from == to) return ExchangeQuote("1", date.toString())
        prepare(setOf(date), setOf(from, to))
        val rows = database.exchangeRateDao().rows()
        fun rate(currency: String): ExchangeRateEntity {
            if (currency == "USD") return ExchangeRateEntity(date.toString(), "USD", "1")
            val candidates = rows.filter { it.currency == currency }
            return candidates.lastOrNull { it.date <= date.toString() } ?: candidates.firstOrNull()
                ?: error("缺少 $currency 汇率，请联网刷新或在常规中填写")
        }
        val source = rate(from)
        val target = rate(to)
        val ratio = target.perUsd.toBigDecimal().divide(source.perUsd.toBigDecimal(), 18, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return ExchangeQuote(ratio, minOf(source.date, target.date))
    }

    suspend fun setManualRate(from: String, to: String, rate: String, date: LocalDate = LocalDate.now()) = mutex.withLock {
        require(from != to && from.matches(Regex("[A-Z]{3}")) && to in cn.turingzy.ledger.model.settings.supportedCurrencyCodes)
        val value = rate.toBigDecimalOrNull()
        require(value != null && value.signum() > 0 && value.scale() in -18..18 && value.precision() <= 36) { "请输入有效汇率" }
        val rows = database.exchangeRateDao().rows()
        val target = if (to == "USD") BigDecimal.ONE else if (from == "USD") value
            else rows.lastOrNull { it.currency == to }?.perUsd?.toBigDecimal() ?: error("请先设置 USD 与 $to 的汇率")
        val entries = if (from == "USD") listOf(ExchangeRateEntity(date.toString(), to, value.toPlainString(), true))
            else listOf(ExchangeRateEntity(date.toString(), from, target.divide(value, 18, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(), true))
        database.exchangeRateDao().insert(entries)
        attempts.clear()
        lastFailureAt = 0L
        _changes.value++
    }
}
