package cn.turingzy.ledger.model.settings

import java.util.Currency
import java.util.Locale

/**
 * 应用全局偏好设置模型。
 *
 * 存储用户对界面语言、主题模式以及主币种的偏好选择。
 *
 * @property language 界面语言选项（如 "system"、"zh"、"en"）
 * @property theme 主题外观模式（如 "system"、"light"、"dark"）
 * @property primaryCurrency 默认主币种代码，符合 ISO 4217
 */
@kotlinx.serialization.Serializable
data class AppPreferences(
    val language: String = "system",
    val theme: String = "system",
    val primaryCurrency: String = defaultPrimaryCurrency(),
)

/**
 * 应用支持的有效法定货币代码集合。
 *
 * 惰性初始化，基于 JVM 可用货币列表过滤出有效币种。
 */
val supportedCurrencyCodes: Set<String> by lazy {
    Currency.getAvailableCurrencies().filter { it.defaultFractionDigits >= 0 }.map { it.currencyCode }.toSet()
}

/**
 * 根据语言环境推导默认的主货币代码。
 *
 * 尝试通过 [Locale.getDefault] 获取地区对应货币，若无法获取或不在支持列表中则回退为 "USD"。
 *
 * @param locale 目标语言环境，默认为当前系统的 [Locale.getDefault]
 * @return 3 位大写货币代码（如 "CNY"、"USD"）
 */
fun defaultPrimaryCurrency(locale: Locale = Locale.getDefault()): String =
    if (locale.country.isBlank()) "USD"
    else runCatching { Currency.getInstance(locale)?.currencyCode }
        .getOrNull()?.takeIf { it in supportedCurrencyCodes } ?: "USD"
