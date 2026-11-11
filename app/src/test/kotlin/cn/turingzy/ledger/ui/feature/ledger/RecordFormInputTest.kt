package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordFormInputTest {
    private val accounts = listOf(
        AccountWithTemplate(Account(id = 1L, name = "现金", type = AccountType.CASH)),
        AccountWithTemplate(Account(id = 2L, name = "银行卡", type = AccountType.DEBIT)),
    )
    private val categories = listOf(
        CategoryWithIcon(Category(id = 5L, name = "自定义餐饮", type = CategoryType.EXPENSE, iconId = 1L), Icon(id = 1L, keyName = "ic_food", displayName = "", iconUri = "")),
        CategoryWithIcon(Category(id = 11L, name = "转账", type = CategoryType.TRANSFER, iconId = 9L), Icon(id = 9L, keyName = "ic_transfer", displayName = "", iconUri = "")),
    )
    private val input = RecordFormInput(TransactionType.EXPENSE, "12.34", 1L, null, 5L, 1_700_000_000_000L, " 晚餐 ")

    @Test
    fun categoryChoicesExcludeOtherAndDeletedCategoriesAndKeepTypeOrder() {
        val template = categories.first()
        val otherExpense = template.copy(category = template.category.copy(id = 20L, name = "其他支出"))
        val otherIncome = template.copy(category = template.category.copy(id = 21L, name = "其他收入", type = CategoryType.INCOME))
        val deleted = template.copy(category = template.category.copy(id = 22L, isDeleted = true))
        val earlier = template.copy(category = template.category.copy(id = 23L, name = "早餐", sortOrder = -1))
        val income = template.copy(category = template.category.copy(id = 24L, name = "工资", type = CategoryType.INCOME))
        val other = template.copy(category = template.category.copy(id = 25L, name = "其他分类"))
        val options = categories + listOf(otherExpense, otherIncome, deleted, earlier, income, other)

        assertEquals(listOf(23L, 5L), selectableRecordCategories(options, TransactionType.EXPENSE).map { it.category.id })
        assertEquals(listOf(24L), selectableRecordCategories(options, TransactionType.INCOME).map { it.category.id })
        assertEquals(listOf(11L), selectableRecordCategories(options, TransactionType.TRANSFER).map { it.category.id })
    }

    @Test
    fun existingTransactionCanKeepItsHiddenOtherCategoryWhenEdited() {
        val other = categories.first().let { it.copy(category = it.category.copy(id = 20L, name = "其他支出")) }
        val transaction = input.copy(categoryId = 20L).toTransactionOrNull(accounts, categories + other)
        assertEquals(20L, checkNotNull(transaction).categoryId)
    }

    @Test
    fun selectedCategoryAndTimestampArePreservedWithoutNameMatching() {
        val transaction = checkNotNull(input.toTransactionOrNull(accounts, categories))
        assertEquals(1234L, transaction.amount)
        assertEquals(5L, transaction.categoryId)
        assertEquals(input.transactionTime, transaction.transactionTime)
        assertEquals("晚餐", transaction.note)
    }

    @Test
    fun missingOrMismatchedRequiredFieldsPreventSaving() {
        assertNull(input.copy(expression = "").toTransactionOrNull(accounts, categories))
        assertNull(input.copy(expression = "0").toTransactionOrNull(accounts, categories))
        assertNull(input.copy(expression = "92233720368547758.08").toTransactionOrNull(accounts, categories))
        assertNull(input.copy(accountId = 99L).toTransactionOrNull(accounts, categories))
        assertNull(input.copy(categoryId = 11L).toTransactionOrNull(accounts, categories))
    }

    @Test
    fun transferUsesExplicitDestinationAndRejectsSameAccount() {
        val transfer = input.copy(type = TransactionType.TRANSFER, categoryId = 11L, toAccountId = 2L)
        assertEquals(2L, checkNotNull(transfer.toTransactionOrNull(accounts, categories)).toAccountId)
        assertNull(transfer.copy(toAccountId = 1L).toTransactionOrNull(accounts, categories))
        assertNull(transfer.copy(toAccountId = 99L).toTransactionOrNull(accounts, categories))
        assertNull(transfer.copy(toAccountId = null).toTransactionOrNull(accounts, categories))
    }

    @Test
    fun chosenLocalTimeUsesDeviceTimezoneAndKeepsMidnightDate() {
        val timestamp = recordTimeMillis(LocalDate.of(2026, 11, 9), "00", "05", ZoneId.of("Asia/Shanghai"))
        assertEquals(Instant.parse("2026-11-08T16:05:00Z").toEpochMilli(), timestamp)
    }

    @Test
    fun invalidTimeCannotBeConfirmed() {
        val date = LocalDate.of(2026, 11, 9)
        val zone = ZoneId.of("Asia/Shanghai")
        assertNull(recordTimeMillis(date, "", "30", zone))
        assertNull(recordTimeMillis(date, "24", "00", zone))
        assertNull(recordTimeMillis(date, "12", "60", zone))
        assertNull(recordTimeMillis(date, "12", "-1", zone))
    }
    @Test
    fun timeLabelOmitsTodayAndKeepsOtherDates() {
        val zone = ZoneId.of("Asia/Shanghai")
        val today = LocalDate.of(2026, 11, 11)
        fun timestamp(date: LocalDate) = date.atTime(13, 17).atZone(zone).toInstant().toEpochMilli()
        assertEquals("13:17", recordTimeLabel(timestamp(today), zone, today))
        assertEquals("11.10 13:17", recordTimeLabel(timestamp(today.minusDays(1)), zone, today))
        assertEquals("2025.11.11 13:17", recordTimeLabel(timestamp(today.minusYears(1)), zone, today))
    }

    @Test
    fun quickMonthSelectionClampsDayForLeapAndShortMonths() {
        assertEquals(LocalDate.of(2024, 2, 29), recordDateInMonth(LocalDate.of(2024, 1, 31), YearMonth.of(2024, 2)))
        assertEquals(LocalDate.of(2025, 2, 28), recordDateInMonth(LocalDate.of(2024, 2, 29), YearMonth.of(2025, 2)))
        assertEquals(LocalDate.of(2024, 4, 30), recordDateInMonth(LocalDate.of(2024, 3, 31), YearMonth.of(2024, 4)))
    }

}
