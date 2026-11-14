package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.*
import org.junit.Assert.*
import org.junit.Test

class BillImportPlannerTest {
    private val rule = BillParseRule.defaults().first()
    private val accounts = listOf(Account(1, "现金", AccountType.CASH), Account(2, "银行卡", AccountType.DEBIT))
    private val categories = listOf(Category(1, "其他支出", CategoryType.EXPENSE, 1), Category(2, "其他收入", CategoryType.INCOME, 1), Category(3, "转账", CategoryType.TRANSFER, 1))
    private val header = listOf("交易时间", "金额", "收支类型", "分类", "备注", "账户", "转入账户", "币种")
    private fun row(type: String, amount: String = "0.01", account: String = "现金", target: String = "") = listOf("2026-11-14 12:00:00", amount, type, "", "备注", account, target, "CNY")
    private fun plan(rows: List<List<String>>, custom: BillParseRule = rule) = BillImportPlanner.plan(listOf(header) + rows, custom, null, accounts, categories)

    @Test fun importsAllTypesAndResolvesExportedAccounts() {
        val planned = plan(listOf(row("支出"), row("收入", "12.34"), row("转账", "100", target = "银行卡")))
        assertEquals(listOf(1L, 1234L, 10000L), planned.map { it.amount })
        assertEquals(listOf(1L, 2L, 3L), planned.map { it.categoryId })
        assertEquals(2L, planned.last().toAccountId)
    }
    @Test fun invalidLaterRowRejectsWholePlan() {
        val error = assertThrows(IllegalArgumentException::class.java) { plan(listOf(row("支出"), row("支出", "无效金额"))) }
        assertTrue(error.message.orEmpty().contains("第 3 行"))
    }
    @Test fun rejectsMissingAccountAndAmbiguousTransfers() {
        assertThrows(IllegalArgumentException::class.java) { plan(listOf(row("支出", account = "不存在"))) }
        assertThrows(IllegalArgumentException::class.java) { plan(listOf(row("转账", target = "现金"))) }
        assertThrows(IllegalArgumentException::class.java) { plan(listOf(row("转账"))) }
    }
    @Test fun rejectsMoneyOverflowZeroAndMismatchedCategory() {
        assertThrows(IllegalArgumentException::class.java) { plan(listOf(row("支出", "92233720368547758.08"))) }
        assertThrows(IllegalArgumentException::class.java) { plan(listOf(row("支出", "0"))) }
        assertThrows(IllegalArgumentException::class.java) { plan(listOf(row("收入")), rule.copy(categoryId = 1)) }
    }
    @Test fun startRowSkipsPreambleAndDefaultAccountOverridesSource() {
        val rows = listOf(listOf("账单明细"), header, row("支出", account = "不存在"))
        val planned = BillImportPlanner.plan(rows, rule.copy(startRow = "3", accountId = 2), null, accounts, categories)
        assertEquals(2L, planned.single().accountId)
    }
    @Test fun duplicateHeadersAndUnknownTypesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { BillImportPlanner.plan(listOf(header + "金额", row("支出")), rule, 1, accounts, categories) }
        assertThrows(IllegalArgumentException::class.java) { plan(listOf(row("不计收支"))) }
    }
}
