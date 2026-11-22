package cn.turingzy.ledger.data.repository.currency

import cn.turingzy.ledger.data.transfer.readBytesLimited
import cn.turingzy.ledger.data.database.entity.ExchangeRateEntity
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * 远程汇率数据源接口。
 */
interface ExchangeRateSource {
    /**
     * 获取指定日期范围及币种集合的汇率实体列表。
     *
     * @param start 起始日期（含）
     * @param end 截止日期（含）
     * @param currencies 需要查询的币种代码集合
     * @return 汇率实体列表
     */
    suspend fun fetch(start: LocalDate, end: LocalDate, currencies: Set<String>): List<ExchangeRateEntity>
}

/**
 * 基于 Frankfurter API 的汇率数据源实现。
 *
 * 通过 Frankfurter 开放 API 获取以 USD 为基准的历史汇率。
 *
 * @param dispatcher 用于网络请求的 IO 调度器
 */
class FrankfurterExchangeRateSource @Inject constructor(
    @param:Dispatcher(LedgerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
) : ExchangeRateSource {
    /**
     * 发送 HTTP 请求从 Frankfurter API 拉取汇率数据，并解析为 [ExchangeRateEntity] 列表。
     *
     * @param start 起始日期
     * @param end 截止日期
     * @param currencies 货币代码集合
     */
    override suspend fun fetch(start: LocalDate, end: LocalDate, currencies: Set<String>): List<ExchangeRateEntity> = withContext(dispatcher) {
        require(currencies.all { it.matches(Regex("[A-Z]{3}")) })
        val quotes = (currencies - "USD").sorted().joinToString(",")
        if (quotes.isEmpty()) return@withContext emptyList()
        val connection = URL("https://api.frankfurter.dev/v2/rates?base=USD&quotes=$quotes&from=$start&to=$end").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Ledger-Android")
            check(connection.responseCode == 200) { "汇率获取失败（${connection.responseCode}），可在常规中手动填写" }
            val body = connection.inputStream.use { it.readBytesLimited(8 * 1024 * 1024).toString(Charsets.UTF_8) }
            currentCoroutineContext().ensureActive()
            Json.parseToJsonElement(body).jsonArray.map { item ->
                val row = item.jsonObject
                check(row.getValue("base").jsonPrimitive.content.uppercase() == "USD") { "汇率基准无效" }
                val date = row.getValue("date").jsonPrimitive.content
                val currency = row.getValue("quote").jsonPrimitive.content.uppercase()
                val rate = row.getValue("rate").jsonPrimitive.content
                require(LocalDate.parse(date) <= end && currency in currencies && rate.toBigDecimal().signum() > 0) { "汇率数据无效" }
                ExchangeRateEntity(date, currency, rate)
            }
        } finally { connection.disconnect() }
    }
}
