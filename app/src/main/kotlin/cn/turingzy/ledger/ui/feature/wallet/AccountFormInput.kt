package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.util.AmountFormatter
import java.util.Currency
import java.util.Locale

internal val accountCurrencyOptions: List<String> by lazy {
    Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
}

internal val accountCurrencyCodes: Set<String> by lazy {
    accountCurrencyOptions.toSet()
}

internal data class AccountFormInput(
    val name: String = "",
    val type: AccountType = AccountType.DEBIT,
    val balance: String = "",
    val cardNumber: String = "",
    val currency: String = "CNY",
    val templateId: Long? = null,
    val includeInTotal: Boolean = true,
) {
    val balanceCents: Long?
        get() = if (balance.isBlank()) 0L else AmountFormatter.parseToCents(balance)

    val currencyCode: String
        get() = currency.trim().uppercase(Locale.ROOT)

    val requiresCardTail: Boolean
        get() = type == AccountType.DEBIT || type == AccountType.CREDIT

    val normalizedCardNumber: String?
        get() = if (requiresCardTail) cardNumber.filterNot { it.isWhitespace() }.ifBlank { null } else null

    val cardNumberValid: Boolean
        get() = normalizedCardNumber?.let { number -> number.length <= 19 && number.all { it in '0'..'9' } } ?: true

    val canSelectCardFace: Boolean
        get() = name.isNotBlank() && (!requiresCardTail || (cardNumberValid && (normalizedCardNumber?.length ?: 0) >= 4))

    fun toAccountOrNull(templates: List<CardTemplate>): Account? {
        if (name.isBlank() || currencyCode !in accountCurrencyCodes || !cardNumberValid) return null
        if (templateId != null && templates.none { it.id == templateId && !it.isDeleted }) return null
        val initialBalance = balanceCents ?: return null
        return Account(
            name = name.trim(),
            type = type,
            initialBalance = initialBalance,
            currentBalance = initialBalance,
            currency = currencyCode,
            templateId = templateId,
            cardNumber = normalizedCardNumber,
            includeInTotal = includeInTotal,
        )
    }

    fun toUpdatedAccountOrNull(original: Account, templates: List<CardTemplate>): Account? {
        val edited = copy(currency = original.currency, balance = "").toAccountOrNull(templates) ?: return null
        return original.copy(
            name = edited.name,
            type = edited.type,
            templateId = edited.templateId,
            cardNumber = edited.cardNumber,
            includeInTotal = edited.includeInTotal,
        )
    }
}
