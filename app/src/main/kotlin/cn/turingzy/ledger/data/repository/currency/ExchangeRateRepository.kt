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

/**
 * 汇率报价数据。
 *
 * 将数据源汇率对应的生效日期与本地最近更新时间戳明确区分。
 *
 * @property rate 汇率比率字符串（目标币种 / 源币种）
 * @property date 汇率生效日期（格式：YYYY-MM-DD）
 * @property updatedAtMillis 本地记录最近更新的毫秒时间戳，手动设置或未记录时可能为 null
 */
data class ExchangeQuote(val rate: String, val date: String, val updatedAtMillis: Long? = null)

/**
 * 汇率数据仓库。
 *
 * 负责汇率数据的本地缓存、在线按需同步、交叉汇率换算与手动汇率覆盖管理。
 *
 * @property database 账本本地数据库
 * @property source 在线汇率数据源接口
 */
@Singleton
class ExchangeRateRepository @Inject constructor(private val database: LedgerDatabase, private val source: ExchangeRateSource) {
    private val mutex = Mutex()
    private val attempts = mutableSetOf<String>()
    private var lastFailureAt = 0L
    private val _changes = MutableStateFlow(0L)

    /**
     * 汇率变动通知流。每当汇率数据发生变更时，计数器递增发射新值。
     */
    val changes = _changes.asStateFlow()

    /**
     * 清除尝试记录与失败冷却状态，并触发汇率变动通知，促使下游重新计算。
     */
    suspend fun invalidate() = mutex.withLock { attempts.clear(); lastFailureAt = 0L; _changes.value++ }

    /**
     * 预热并准备指定日期与币种列表所需的汇率数据。
     *
     * 检查本地缺失的历史汇率，从远端数据源拉取并持久化至数据库。支持失败限流与重复请求去重。
     *
     * @param dates 需要汇率的业务日期集合
     * @param currencies 需要的货币代码集合
     * @param refresh 是否强制从远端刷新最新汇率（忽略冷却与已有缓存）
     */
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
            val updatedAt = System.currentTimeMillis()
            val updates = rows.mapNotNull { row ->
                val cached = byKey[row.date to row.currency]
                if (cached?.manual == true) null
                else if (refresh || cached?.updatedAtMillis == null || cached.perUsd != row.perUsd) {
                    row.copy(updatedAtMillis = updatedAt)
                } else null
            }
            if (updates.isNotEmpty()) {
                database.exchangeRateDao().insert(updates)
                if (updates.any { byKey[it.date to it.currency]?.perUsd != it.perUsd }) _changes.value++
            }
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            lastFailureAt = System.currentTimeMillis()
            if (refresh) throw error
            Timber.w(error, "Unable to refresh exchange rates; using cached values")
        }
    }

    /**
     * 查询指定日期下两个币种之间的汇率报价。
     *
     * 若两者相同直接返回比率 1；否则以 USD 作为基准桥梁换算交叉汇率。
     *
     * @param from 源币种代码（如 "CNY"）
     * @param to 目标币种代码（如 "USD"）
     * @param date 交易或查询日期
     * @return 包含转换汇率、生效日期及更新时间的 [ExchangeQuote]
     * @throws IllegalStateException 当本地缺少所需币种的汇率时抛出
     */
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
        val updatedAt = listOfNotNull(source.updatedAtMillis, target.updatedAtMillis).maxOrNull()
        return ExchangeQuote(ratio, minOf(source.date, target.date), updatedAt)
    }

    /**
     * 手动录入或覆盖特定币种之间的汇率。
     *
     * 会折算并持久化为基于 USD 的汇率记录，同时清空冷却记录并通知下游汇率变更。
     *
     * @param from 源币种代码（3位字母大写）
     * @param to 目标币种代码（必须为系统支持的币种）
     * @param rate 汇率数值字符串（必须为正数，且精度在合理范围内）
     * @param date 汇率生效日期，默认为当天
     */
    suspend fun setManualRate(from: String, to: String, rate: String, date: LocalDate = LocalDate.now()) = mutex.withLock {
        require(from != to && from.matches(Regex("[A-Z]{3}")) && to in cn.turingzy.ledger.model.settings.supportedCurrencyCodes)
        val value = rate.toBigDecimalOrNull()
        require(value != null && value.signum() > 0 && value.scale() in -18..18 && value.precision() <= 36) { "请输入有效汇率" }
        val rows = database.exchangeRateDao().rows()
        val target = if (to == "USD") BigDecimal.ONE else if (from == "USD") value
            else rows.lastOrNull { it.currency == to }?.perUsd?.toBigDecimal() ?: error("请先设置 USD 与 $to 的汇率")
        val perUsd = if (from == "USD") value
            else target.divide(value, 18, RoundingMode.HALF_UP).stripTrailingZeros()
        val entry = ExchangeRateEntity(
            date.toString(), if (from == "USD") to else from, perUsd.toPlainString(),
            manual = true, updatedAtMillis = System.currentTimeMillis(),
        )
        database.exchangeRateDao().insert(listOf(entry))
        attempts.clear()
        lastFailureAt = 0L
        _changes.value++
    }
}
