package cn.turingzy.ledger.data.debug

import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal data class DebugScenarioData(val accounts: List<Account>, val transactions: List<Transaction>)

internal fun debugScenarioData(
    now: Instant,
    zone: ZoneId,
    expenseCategoryIds: List<Long>,
    incomeCategoryIds: List<Long>,
    transferCategoryId: Long,
): DebugScenarioData {
    require(expenseCategoryIds.isNotEmpty() && incomeCategoryIds.isNotEmpty())
    val accounts = listOf(
        Account(-11001L, "场景测试·人民币现金", AccountType.CASH, initialBalance = 20_000L, templateId = 3),
        Account(-11002L, "场景测试·人民币工资卡", AccountType.DEBIT, initialBalance = 5_000_000L, templateId = 1, cardNumber = "6228"),
        Account(-11003L, "场景测试·人民币钱包", AccountType.WALLET, initialBalance = 80_000L, templateId = 3),
        Account(-11004L, "场景测试·人民币信用卡", AccountType.CREDIT, initialBalance = -168_000L, templateId = 2, cardNumber = "8899"),
        Account(-11005L, "场景测试·美元银行卡", AccountType.DEBIT, initialBalance = 500_000L, currency = "USD", templateId = 1, cardNumber = "1234"),
        Account(-11006L, "场景测试·美元现金", AccountType.CASH, initialBalance = 50_000L, currency = "USD", templateId = 3),
        Account(-11007L, "场景测试·港币钱包", AccountType.WALLET, initialBalance = 200_000L, currency = "HKD", templateId = 3),
        Account(-11008L, "场景测试·欧元空账户", AccountType.DEBIT, currency = "EUR", templateId = 1, cardNumber = "5678"),
        Account(-11009L, "场景测试·已删除旧卡", AccountType.DEBIT, initialBalance = 60_000L, templateId = 2, cardNumber = "0001", isDeleted = true),
        Account(-11010L, "场景测试·不计入总资产", AccountType.DEBIT, initialBalance = 3_000_000L, templateId = 2, cardNumber = "9000", includeInTotal = false),
    )
    val today = now.atZone(zone).toLocalDate()
    val entries = buildList {
        fun addEntry(
            date: LocalDate, accountId: Long, amount: Long, type: TransactionType, categoryId: Long,
            note: String, toAccountId: Long? = null, importKey: String? = null, deleted: Boolean = false,
        ) {
            add(Transaction(
                type = type, amount = amount, accountId = accountId, toAccountId = toAccountId,
                categoryId = categoryId,
                transactionTime = minOf(date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(), now.toEpochMilli() - 60_000L),
                note = "[场景测试] $note", importKey = importKey, isDeleted = deleted,
            ))
        }
        repeat(24) { offset ->
            val month = YearMonth.from(today).minusMonths(offset.toLong())
            val date = month.atDay(1)
            val expenseCategory = expenseCategoryIds[offset % expenseCategoryIds.size]
            val incomeCategory = incomeCategoryIds[offset % incomeCategoryIds.size]
            addEntry(date, -11002L, 1_200_000L + offset * 10_000L, TransactionType.INCOME, incomeCategory, "工资 ${month}")
            addEntry(date, -11002L, 250_000L + offset * 1000L, TransactionType.EXPENSE, expenseCategory, "房租 ${month}")
            addEntry(date, -11003L, 3_800L + offset * 100L, TransactionType.EXPENSE, expenseCategory, "日常采购 ${month}")
            addEntry(date, -11001L, 1_200L, TransactionType.EXPENSE, expenseCategory, "早餐 ${month}")
            addEntry(date, -11004L, 16_800L, TransactionType.EXPENSE, expenseCategory, "信用卡购物 ${month}")
            addEntry(date, -11005L, 180_000L, TransactionType.INCOME, incomeCategory, "海外项目 ${month}")
            addEntry(date, -11005L, 25_000L + offset * 100L, TransactionType.EXPENSE, expenseCategory, "美元订阅 ${month}")
            addEntry(date, -11006L, 850L, TransactionType.EXPENSE, expenseCategory, "海外咖啡 ${month}")
            addEntry(date, -11007L, 50_000L, TransactionType.INCOME, incomeCategory, "港币收入 ${month}")
            addEntry(date, -11007L, 8_800L + offset * 100L, TransactionType.EXPENSE, expenseCategory, "港币交通 ${month}")
            addEntry(date, -11002L, 20_000L, TransactionType.TRANSFER, transferCategoryId, "钱包充值 ${month}", -11003L)
            addEntry(date, -11005L, 5_000L, TransactionType.TRANSFER, transferCategoryId, "美元取现 ${month}", -11006L)
        }
        repeat(14) { index ->
            val date = today.minusDays(index.toLong())
            val category = expenseCategoryIds[index % expenseCategoryIds.size]
            addEntry(date, -11003L, 100L + index * index * 137L, TransactionType.EXPENSE, category, "近两周消费 ${index + 1}")
            addEntry(date, -11006L, 250L + index * 75L, TransactionType.EXPENSE, category, "跨币种搜索 海外午餐 ${index + 1}")
        }
        addEntry(today, -11001L, 1L, TransactionType.EXPENSE, expenseCategoryIds.first(), "一分钱小额")
        addEntry(today.minusDays(2), -11002L, 1_234_567L, TransactionType.EXPENSE, expenseCategoryIds.last(), "大额设备采购")
        addEntry(today.minusYears(2).withDayOfYear(1), -11009L, 2_500L, TransactionType.EXPENSE, expenseCategoryIds.first(), "跨年搜索 旧卡历史")
        addEntry(today, -11003L, 9_999L, TransactionType.EXPENSE, expenseCategoryIds.first(), "已删除交易不应显示", deleted = true)
        val importDate = LocalDate.of(2026, 1, 2)
        addEntry(importDate, -11003L, 1_800L, TransactionType.EXPENSE, expenseCategoryIds.first(), "重复导入咖啡", importKey = "交易号:scenario-coffee-001")
        addEntry(importDate, -11003L, 1_800L, TransactionType.EXPENSE, expenseCategoryIds.first(), "重复导入咖啡", importKey = "交易号:scenario-coffee-002")
        addEntry(importDate, -11003L, 2_800L, TransactionType.EXPENSE, expenseCategoryIds.first(), "手工午餐待匹配")
    }
    return DebugScenarioData(accounts.map { account ->
        val balance = entries.filterNot { it.isDeleted }.fold(account.initialBalance) { balance, entry ->
            val outgoing = if (entry.accountId != account.id) 0L else if (entry.type == TransactionType.INCOME) entry.amount else -entry.amount
            val incoming = if (entry.toAccountId == account.id) entry.amount else 0L
            Math.addExact(Math.addExact(balance, outgoing), incoming)
        }
        account.copy(currentBalance = balance)
    }, entries)
}
