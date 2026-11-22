package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
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
) {
    /** 将输入的余额文本解析为分单位金额；为空时默认为 0，格式不合法时返回 null */
    val balanceCents: Long?
        get() = if (balance.isBlank()) 0L else AmountFormatter.parseToCents(balance)

    /** 规范化后的大写币种代码 */
    val currencyCode: String
        get() = currency.trim().uppercase(Locale.ROOT)

    /** 当前账户类型是否需要录入卡号/尾号（储蓄卡与信用卡需要） */
    val requiresCardTail: Boolean
        get() = type == AccountType.DEBIT || type == AccountType.CREDIT

    /** 去除空白字符后的卡号字符串；不需要卡号时返回 null */
    val normalizedCardNumber: String?
        get() = if (requiresCardTail) cardNumber.filterNot { it.isWhitespace() }.ifBlank { null } else null

    /** 卡号格式是否合法（非空时必须全部为数字且长度不超过 19 位） */
    val cardNumberValid: Boolean
        get() = normalizedCardNumber?.let { number -> number.length <= 19 && number.all { it in '0'..'9' } } ?: true

    /** 是否满足选择卡面样式的先决条件（名称非空，若需卡号则卡号必须合法且至少 4 位） */
    val canSelectCardFace: Boolean
        get() = name.isNotBlank() && (!requiresCardTail || (cardNumberValid && (normalizedCardNumber?.length ?: 0) >= 4))

    /**
     * 校验当前输入并构建新的 [Account] 对象；校验失败时返回 null。
     *
     * @param templates 用于校验 templateId 的卡面模板列表
     * @return 构建成功的新账户对象，或 null
     */
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
            includeInTotal = edited.includeInTotal,
        )
    }
}
