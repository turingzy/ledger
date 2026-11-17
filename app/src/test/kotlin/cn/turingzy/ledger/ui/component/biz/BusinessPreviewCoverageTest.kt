package cn.turingzy.ledger.ui.component.biz

import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.transaction.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessPreviewCoverageTest {
    @Test
    fun amountPreviewsCoverEveryTypeSizeAndFormattingBranch() {
        val cases = AmountPreviewProvider().values.toList()
        assertTrue(cases.mapNotNull { it.type }.containsAll(TransactionType.entries))
        assertTrue(cases.map { it.size }.containsAll(BusinessAmountSize.entries))
        assertTrue(cases.any { it.currency == it.primary })
        assertTrue(cases.any { it.currency != it.primary })
        assertTrue(cases.any { it.currency == "ZZZ" })
        assertTrue(cases.any { it.amount == null && !it.hidden })
        assertTrue(cases.any { it.amount == null && it.hidden })
        assertTrue(cases.any { it.amount != null && it.hidden })
        assertTrue(cases.any { it.amount == 0L })
        assertTrue(cases.any { it.amount == Long.MIN_VALUE })
        assertTrue(cases.any { it.amount == Long.MAX_VALUE })
        assertTrue(cases.any { it.grouping })
    }

    @Test
    fun accountIconPreviewsCoverEveryTypeAndBrandAccent() {
        val cases = AccountPreviewProvider().values.toList()
        assertTrue(cases.map { it.account.type }.containsAll(AccountType.entries))
        assertTrue(cases.map { it.account.name }.containsAll(listOf("支付宝", "微信", "其他钱包")))
    }

    @Test
    fun categoryIconPreviewsCoverOfficialMappingsAndTypeFallbacks() {
        val cases = CategoryPreviewProvider().values.toList()
        val keys = cases.mapNotNull { it.category?.icon?.keyName }.toSet()
        assertTrue(keys.containsAll(listOf("ic_food", "ic_shopping", "ic_transport", "ic_entertainment", "ic_housing",
            "ic_medical", "ic_salary", "ic_investment", "ic_transfer", "ic_other", "unknown")))
        assertEquals(TransactionType.entries.toSet(), cases.filter { it.category == null }.map { it.type }.toSet())
    }

    @Test
    fun transactionPreviewsCoverHistoricalReferencesAndIncompleteTransfers() {
        val cases = TransactionPreviewProvider().values.toList()
        assertEquals(TransactionType.entries.toSet(), cases.map { it.detail.transaction.type }.toSet())
        assertTrue(cases.any { it.detail.transaction.isDeleted })
        assertTrue(cases.any { it.detail.account.isDeleted })
        assertTrue(cases.any { it.detail.toAccount?.isDeleted == true })
        assertTrue(cases.any { it.detail.category.category.isDeleted })
        assertTrue(cases.any { it.hidden })
        val transfers = cases.filter { it.detail.transaction.type == TransactionType.TRANSFER }
        assertTrue(transfers.any { it.detail.toAccount == null })
        assertTrue(transfers.any { it.detail.toAccount?.currency != it.detail.account.currency && it.detail.transaction.toAmount == null })
        assertTrue(transfers.any { it.detail.toAccount?.currency != it.detail.account.currency && it.detail.transaction.toAmount != null })
        assertTrue(transfers.any { it.detail.toAccount?.currency == it.detail.account.currency && it.detail.transaction.toAmount?.let { received -> received != it.detail.transaction.amount } == true })
    }
}
