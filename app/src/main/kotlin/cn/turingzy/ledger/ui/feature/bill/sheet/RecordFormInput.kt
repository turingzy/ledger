package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.util.AmountFormatter

/**
 * 记账表单录入数据载体。
 *
 * @property type 交易类型（支出、收入、转账）。
 * @property expression 金额输入框的原始表达式字符串（可能包含加减乘除运算符）。
 * @property accountId 源账户或支出账户 ID。
 * @property toAccountId 转入账户 ID（仅转账交易有效）。
 * @property categoryId 选中的分类 ID。
 * @property transactionTime 交易发生时间戳（毫秒）。
 * @property note 交易备注。
 * @property transferRate 跨币种转账时的折算汇率字符串。
 */
internal data class RecordFormInput(
    val type: TransactionType,
    val expression: String,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long,
    val transactionTime: Long,
    val note: String,
    val transferRate: String? = null,
) {
    /**
     * 校验表单数据的合法性，并转换为可持久化的 [Transaction] 实体。
     *
     * 校验规则包含：金额合法且大于 0、账户与分类存在且未删除、转账目标账户合法且不与源账户相同、
     * 跨币种转账时汇率有效且折算金额大于 0。
     *
     * @param accounts 系统当前有效账户列表。
     * @param categories 系统当前有效分类列表。
     * @return 校验通过返回 [Transaction]，失败返回 null。
     */
    fun toTransactionOrNull(accounts: List<AccountWithTemplate>, categories: List<CategoryWithIcon>): Transaction? {
        val amount = AmountFormatter.parseToCents(evaluateMathExpression(expression))?.takeIf { it > 0L } ?: return null
        val account = accounts.firstOrNull { it.account.id == accountId && !it.account.isDeleted }?.account ?: return null
        if (categories.none { it.category.id == categoryId && it.category.type == type.categoryType() && !it.category.isDeleted }) return null
        val destination = if (type == TransactionType.TRANSFER) {
            toAccountId?.takeIf { id -> id != accountId && accounts.any { it.account.id == id && !it.account.isDeleted } } ?: return null
        } else null
        val target = accounts.firstOrNull { it.account.id == destination }?.account
        val converted = if (target == null) null else if (target.currency == account.currency) amount else {
            val rate = transferRate ?: return null
            try { cn.turingzy.ledger.model.currency.convertCents(amount, rate).takeIf { it > 0L } ?: return null }
            catch (_: NumberFormatException) { return null }
            catch (_: ArithmeticException) { return null }
            catch (_: IllegalArgumentException) { return null }
        }
        return Transaction(
            type = type,
            amount = amount,
            accountId = accountId,
            toAccountId = destination,
            categoryId = categoryId,
            transactionTime = transactionTime,
            note = note.trim().takeIf { it.isNotEmpty() },
            toAmount = converted,
            transferRate = transferRate,
        )
    }
}

/**
 * 将业务交易类型 [TransactionType] 映射为对应的分类类型 [CategoryType]。
 */
internal fun TransactionType.categoryType(): CategoryType = when (this) {
    TransactionType.EXPENSE -> CategoryType.EXPENSE
    TransactionType.INCOME -> CategoryType.INCOME
    TransactionType.TRANSFER -> CategoryType.TRANSFER
}

/**
 * 筛选并排序在记账页面可供用户选择的分类列表。
 *
 * 排除已删除分类，并默认过滤掉系统占位的“其他”类分类（转账类型除外）。
 *
 * @param categories 待筛选的分类全量列表。
 * @param type 当前选中的交易类型。
 * @return 过滤并按顺序排序后的 [CategoryWithIcon] 列表。
 */
internal fun selectableRecordCategories(categories: List<CategoryWithIcon>, type: TransactionType): List<CategoryWithIcon> =
    categories.filter {
        it.category.type == type.categoryType() && !it.category.isDeleted &&
            (type == TransactionType.TRANSFER || it.category.name !in OtherRecordCategoryNames)
    }.sortedBy { it.category.sortOrder }

private val OtherRecordCategoryNames = setOf("其他", "其他分类", "其他支出", "其他收入")
