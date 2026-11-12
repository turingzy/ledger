package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AccountFormInputTest {
    @Test
    fun formPreservesRealAccountFieldsAndInitializesCurrentBalance() {
        val template = CardTemplate(id = 42L, name = "自定义卡面", coverUri = "file:///card.png")
        val account = checkNotNull(
            AccountFormInput(
                name = " 工资卡 ",
                type = AccountType.DEBIT,
                balance = "1234.56",
                cardNumber = "1234 5678",
                currency = " usd ",
                templateId = template.id,
                includeInTotal = false,
            ).toAccountOrNull(listOf(template)),
        )
        assertEquals("工资卡", account.name)
        assertEquals(123456L, account.initialBalance)
        assertEquals(account.initialBalance, account.currentBalance)
        assertEquals("12345678", account.cardNumber)
        assertEquals("USD", account.currency)
        assertEquals(42L, account.templateId)
        assertFalse(account.includeInTotal)
    }

    @Test
    fun emptyOptionalFieldsDoNotCreateDemoValues() {
        val account = checkNotNull(AccountFormInput(name = "现金", type = AccountType.CASH).toAccountOrNull(emptyList()))
        assertEquals(0L, account.initialBalance)
        assertNull(account.cardNumber)
        assertNull(account.templateId)
    }

    @Test
    fun nonBankAccountsCanSelectCardFacesWithoutKeepingHiddenCardNumbers() {
        listOf(AccountType.WALLET, AccountType.CASH).forEach { type ->
            val input = AccountFormInput(name = "钱包", type = type)
            assertFalse(input.requiresCardTail)
            assertTrue(input.canSelectCardFace)
            assertFalse(input.copy(name = " ").canSelectCardFace)
            assertNull(checkNotNull(input.copy(cardNumber = "invalid").toAccountOrNull(emptyList())).cardNumber)
        }
    }

    @Test
    fun maximumCentAmountIsExactAndOverflowIsRejected() {
        val input = AccountFormInput(name = "账户", balance = "92233720368547758.07")
        assertEquals(Long.MAX_VALUE, checkNotNull(input.toAccountOrNull(emptyList())).initialBalance)
        assertNull(input.copy(balance = "92233720368547758.08").toAccountOrNull(emptyList()))
    }

    @Test
    fun invalidFieldsCannotBeSavedAsDefaults() {
        val valid = AccountFormInput(name = "账户")
        listOf(
            valid.copy(name = " "),
            valid.copy(balance = "abc"),
            valid.copy(currency = "invalid"),
            valid.copy(cardNumber = "123x"),
            valid.copy(cardNumber = "12345678901234567890"),
            valid.copy(templateId = 42L),
        ).forEach { assertNull(it.toAccountOrNull(emptyList())) }
    }

    @Test
    fun cardFaceSelectionRequiresNameAndACompleteNumericTail() {
        val valid = AccountFormInput(name = "工资卡", cardNumber = "1234")
        assertTrue(valid.canSelectCardFace)
        assertTrue(valid.copy(type = AccountType.CREDIT).canSelectCardFace)
        assertTrue(valid.copy(cardNumber = "1234 5678 9012 3456").canSelectCardFace)
        listOf(
            valid.copy(name = " "),
            valid.copy(cardNumber = ""),
            valid.copy(cardNumber = "123"),
            valid.copy(cardNumber = "12x4"),
        ).forEach { assertFalse(it.canSelectCardFace) }
    }

    @Test
    fun deletedTemplatesCannotBeSelected() {
        val template = CardTemplate(id = 42L, name = "旧卡面", coverUri = "", isDeleted = true)
        assertNull(AccountFormInput(name = "账户", templateId = template.id).toAccountOrNull(listOf(template)))
    }
    @Test
    fun editingChangesMetadataButPreservesIdentityCurrencyAndBalances() {
        val original = Account(
            id = 17L,
            name = "旧名称",
            type = AccountType.DEBIT,
            initialBalance = 10000L,
            currentBalance = 34567L,
            currency = "CNY",
        )
        val edited = checkNotNull(
            AccountFormInput(
                name = "新名称",
                type = AccountType.CREDIT,
                balance = "999.99",
                currency = "USD",
                cardNumber = "1234",
                includeInTotal = false,
            ).toUpdatedAccountOrNull(original, emptyList()),
        )
        assertEquals(original.id, edited.id)
        assertEquals(original.currency, edited.currency)
        assertEquals(original.currentBalance, edited.currentBalance)
        assertEquals("新名称", edited.name)
        assertEquals(AccountType.CREDIT, edited.type)
        assertEquals(original.initialBalance, edited.initialBalance)
        assertEquals("1234", edited.cardNumber)
        assertFalse(edited.includeInTotal)
    }

}
