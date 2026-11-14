package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.*

object BillImportPlanner {
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
                require(target == null || (target.id != account.id && target.currency == account.currency)) { "转账账户或币种无效" }
                Transaction(type = type, amount = kotlin.math.abs(signed), accountId = account.id, toAccountId = target?.id,
                    categoryId = category.id, transactionTime = BillFileCodec.timestamp(field(rule.dateColumn)), note = field(rule.noteColumn).ifEmpty { null })
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
