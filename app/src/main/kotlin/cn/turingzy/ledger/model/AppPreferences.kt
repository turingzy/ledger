package cn.turingzy.ledger.model

import java.util.Currency
import java.util.Locale

@kotlinx.serialization.Serializable
data class AppPreferences(
    val language: String = "system",
    val theme: String = "system",
    val primaryCurrency: String = defaultPrimaryCurrency(),
)

val supportedCurrencyCodes: Set<String> by lazy {
    Currency.getAvailableCurrencies().filter { it.defaultFractionDigits >= 0 }.map { it.currencyCode }.toSet()
}

fun defaultPrimaryCurrency(locale: Locale = Locale.getDefault()): String =
    if (locale.country.isBlank()) "USD"
    else runCatching { Currency.getInstance(locale)?.currencyCode }
        .getOrNull()?.takeIf { it in supportedCurrencyCodes } ?: "USD"
