package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.BankCardDetails
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.isBankCard
import cn.turingzy.ledger.util.AmountFormatter
import java.util.Currency
import java.util.Locale

/**
 * 平台支持的全部可用币种代码列表，按字母升序排序。
 */
internal val accountCurrencyOptions: List<String> by lazy {
    Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
}

/**
 * 平台支持的全部可用币种代码集合，用于快速校验合法性。
 */
internal val accountCurrencyCodes: Set<String> by lazy {
    accountCurrencyOptions.toSet()
}

/**
 * 账户编辑/新增表单输入状态数据类。
 *
 * 封装各字段的原始输入值并提供校验与转换逻辑。
 *
 * @property name 输入的账户名称
 * @property type 选中的账户类型（储蓄卡、信用卡、电子钱包、现金）
 * @property balance 输入的期初余额/负债金额字符串
 * @property cardNumber 输入的银行卡号或尾号字符串
 * @property currency 选中的币种代码（如 "CNY"）
 * @property templateId 选中的卡面模板 ID
 * @property includeInTotal 是否计入总资产
 */
internal data class AccountFormInput(
    val name: String = "",
    val type: AccountType = AccountType.DEBIT,
    val balance: String = "",
    val cardNumber: String = "",
    val currency: String = "CNY",
    val templateId: Long? = null,
    val includeInTotal: Boolean = true,
    val cardExpiry: String = "",
    val cardSecurityCode: String = "",
) {
    /** 将输入的余额文本解析为分单位金额；为空时默认为 0，格式不合法时返回 null */
    val balanceCents: Long?
        get() = if (balance.isBlank()) 0L else AmountFormatter.parseToCents(balance)

    /** 规范化后的大写币种代码 */
    val currencyCode: String
        get() = currency.trim().uppercase(Locale.ROOT)

    /** 当前账户类型是否显示银行卡字段。 */
    val isBankCard: Boolean
        get() = type.isBankCard

    /** 去除空白字符后的卡号字符串；不需要卡号时返回 null */
    val normalizedCardNumber: String?
        get() = if (isBankCard) cardNumber.filterNot { it.isWhitespace() || it == '-' }.ifBlank { null } else null

    /** 完整卡号使用 12 至 19 位数字，也能保留旧账户的四位尾号。 */
    val cardNumberValid: Boolean
        get() = normalizedCardNumber?.let { number ->
            (number.length == 4 || number.length in 12..19) && number.all { it in '0'..'9' }
        } ?: true

    /** 有效期采用 MM/YY，安全码允许 3 或 4 位数字，两者都可留空。 */
    val expiryValid: Boolean
        get() = !isBankCard || cardExpiry.isBlank() || cardExpiry.matches(Regex("(0[1-9]|1[0-2])/[0-9]{2}"))
    val securityCodeValid: Boolean
        get() = !isBankCard || cardSecurityCode.isBlank() || cardSecurityCode.matches(Regex("[0-9]{3,4}"))

    /** 账户已命名，且已填写的卡号格式有效时允许选择卡面。 */
    val canSelectCardFace: Boolean
        get() = name.isNotBlank() && cardNumberValid

    /**
     * 校验当前输入并构建新的 [Account] 对象；校验失败时返回 null。
     *
     * @param templates 用于校验 templateId 的卡面模板列表
     * @return 构建成功的新账户对象，或 null
     */
    fun toAccountOrNull(templates: List<CardTemplate>): Account? {
        if (name.isBlank() || currencyCode !in accountCurrencyCodes || !cardNumberValid || !expiryValid || !securityCodeValid) return null
        if (templateId != null && templates.none { it.id == templateId && !it.isDeleted }) return null
        val initialBalance = balanceCents ?: return null
        val number = normalizedCardNumber
        val details = BankCardDetails(
            number = number?.takeIf { it.length > 4 },
            expiry = cardExpiry.takeIf { isBankCard && it.isNotBlank() },
            securityCode = cardSecurityCode.takeIf { isBankCard && it.isNotBlank() },
        )
        return Account(
            name = name.trim(),
            type = type,
            initialBalance = initialBalance,
            currentBalance = initialBalance,
            currency = currencyCode,
            templateId = templateId,
            cardNumber = number?.takeLast(4),
            cardDetails = details.takeIf { it.number != null || it.expiry != null || it.securityCode != null },
            includeInTotal = includeInTotal,
        )
    }

    /**
     * 校验当前输入并基于原账户生成更新后的 [Account] 对象；校验失败时返回 null。
     *
     * @param original 原账户对象
     * @param templates 用于校验 templateId 的卡面模板列表
     * @return 更新后的账户对象，或 null
     */
    fun toUpdatedAccountOrNull(original: Account, templates: List<CardTemplate>): Account? {
        val newTemplateId = templateId.takeUnless { it == original.templateId }
        val edited = copy(currency = original.currency, balance = "", templateId = newTemplateId)
            .toAccountOrNull(templates) ?: return null
        return original.copy(
            name = edited.name,
            type = edited.type,
            templateId = templateId,
            cardNumber = edited.cardNumber,
            cardDetails = edited.cardDetails,
            includeInTotal = edited.includeInTotal,
        )
    }
}
