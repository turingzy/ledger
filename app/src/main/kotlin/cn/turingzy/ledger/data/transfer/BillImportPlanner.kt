package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.transfer.BillParseRule
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.model.currency.convertCents
import cn.turingzy.ledger.model.settings.supportedCurrencyCodes

/**
 * 账单导入规划器。
 *
 * 负责依据解析规则（[BillParseRule]）对原始表格数据进行表头定位、字段映射、数据转换和完整性校验，
 * 将文本行构建为合法的 [Transaction] 交易列表。
 */
object BillImportPlanner {
    /**
     * 根据解析规则将表格行数据转换为待导入的交易记录列表。
     *
     * 处理逻辑包括：
     * 1. 校验规则有效性并定位表头行（包含交易时间列与金额列）。
     * 2. 逐行解析收支类型、金额、账户、分类、转账信息及多币种汇率换算数据。
     * 3. 提取来源交易流水号生成用于去重的导入键（`importKey`）。
     * 4. 捕获格式错误或非法行并包装为带明确行号的异常信息。
     *
     * @param rows 原始账单二维文本数据。
     * @param rule 账单解析映射规则。
     * @param selectedAccountId 用户在界面上指定的兜底账户 ID（当规则中未显式指定且账单无“账户”列时使用）。
     * @param accounts 当前系统内的账户列表，用于账户查找与币种核验。
     * @param categories 当前系统内的分类列表，用于收支分类匹配。
     * @return 解析完成的 [Transaction] 列表。
     * @throws IllegalArgumentException 当规则无效、缺少必要表头、数据行内容不合法或未匹配到有效账单时抛出。
     */
    fun plan(rows: List<List<String>>, rule: BillParseRule, selectedAccountId: Long?, accounts: List<Account>, categories: List<Category>): List<Transaction> {
        require(rule.valid) { "解析规则无效" }
        val start = requireNotNull(rule.startRow.toIntOrNull()) - 1
        require(start in rows.indices) { "起始行超出文件范围" }
        val headerIndex = (0..start).lastOrNull { index ->
            rows[index].map { it.trim() }.let { rule.dateColumn.trim() in it && rule.amountColumn.trim() in it }
        } ?: error("找不到交易时间或金额列，请检查解析规则和起始行")
        val header = rows[headerIndex].map { it.trim() }
        require(header.filter { it.isNotEmpty() }.distinct().size == header.count { it.isNotEmpty() }) { "账单包含重复列名" }
        val pending = rows.drop(maxOf(start, headerIndex + 1)).mapIndexedNotNull { index, row ->
            if (row.all { it.isBlank() }) return@mapIndexedNotNull null
            try {
                fun field(name: String) = row.getOrNull(header.indexOf(name.trim()))?.trim().orEmpty()
                val signed = BillFileCodec.amount(field(rule.amountColumn))
                require(signed != Long.MIN_VALUE && signed != 0L) { "金额必须大于零" }
                val type = when (val label = field(rule.typeColumn)) {
                    "收入", "收", "贷", "贷方", "INCOME" -> TransactionType.INCOME
                    "支出", "支", "借", "借方", "EXPENSE" -> TransactionType.EXPENSE
                    "转账", "TRANSFER" -> TransactionType.TRANSFER
                    "" -> if (rule.typeColumn.isBlank()) TransactionType.EXPENSE else error("未指定收支类型")
                    else -> error("无法识别收支类型：$label")
                }
                val accountId = rule.accountId ?: selectedAccountId ?: accounts.singleOrNull { it.name == field("账户") }?.id
                    ?: error("请选择导入账户")
                val account = accounts.firstOrNull { it.id == accountId } ?: error("默认账户已删除")
                val currency = field("币种")
                require(currency.isEmpty() || currency == account.currency) { "账单币种与账户不一致" }
                val categoryType = CategoryType.valueOf(type.name)
                val category = if (rule.categoryId != null) categories.firstOrNull { it.id == rule.categoryId && it.type == categoryType }
                    ?: error("默认分类与收支类型不匹配")
                else categories.singleOrNull { it.name == field(rule.categoryColumn) && it.type == categoryType }
                    ?: categories.firstOrNull { it.type == categoryType && it.name in setOf("其他支出", "其他收入", "转账") }
                    ?: error("找不到匹配的分类")
                val target = if (type == TransactionType.TRANSFER) accounts.singleOrNull { it.name == field("转入账户") }
                    ?: error("找不到转入账户") else null
                require(target == null || target.id != account.id) { "转账账户或币种无效" }
                val transferred = if (target == null) null else field("转入金额").takeIf { it.isNotBlank() }?.let(BillFileCodec::amount)
                require(transferred == null || transferred > 0L && (target?.currency != account.currency || transferred == kotlin.math.abs(signed))) { "转入金额无效" }
                val baseCurrency = field("主要货币").takeIf { it.isNotBlank() }
                val baseRate = field("主要货币汇率").takeIf { it.isNotBlank() }
                val baseAmount = field("主要货币金额").takeIf { it.isNotBlank() }?.let(BillFileCodec::amount)
                if (baseCurrency != null || baseRate != null || baseAmount != null) {
                    require(baseCurrency in supportedCurrencyCodes && baseRate != null && baseAmount != null &&
                        convertCents(kotlin.math.abs(signed), baseRate) == baseAmount) { "主要货币换算数据无效" }
                }
                Transaction(type = type, amount = kotlin.math.abs(signed), accountId = account.id, toAccountId = target?.id,
                    categoryId = category.id, transactionTime = BillFileCodec.timestamp(field(rule.dateColumn)), note = field(rule.noteColumn).ifEmpty { null },
                    toAmount = transferred, baseCurrency = baseCurrency, baseRate = baseRate, baseAmount = baseAmount,
                    baseRateDate = field("汇率日期").takeIf { it.isNotBlank() },
                    importKey = field("来源交易号").ifBlank {
                        listOf("交易号", "交易单号", "流水号", "交易流水号").firstNotNullOfOrNull { column ->
                            field(column).takeIf { it.isNotBlank() && it != "-" }?.let { "$column:$it" }
                        }.orEmpty()
                    }.ifBlank { null })
            } catch (error: ArithmeticException) {
                throw IllegalArgumentException("第 ${maxOf(start, headerIndex + 1) + index + 1} 行：金额超出范围或小数位超过两位", error)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("第 ${maxOf(start, headerIndex + 1) + index + 1} 行：${error.message}", error)
            } catch (error: IllegalStateException) {
                throw IllegalArgumentException("第 ${maxOf(start, headerIndex + 1) + index + 1} 行：${error.message}", error)
            }
        }
        require(pending.isNotEmpty()) { "没有可导入的账单" }
        return pending
    }
}
