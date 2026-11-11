package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransactionDetailsSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sheetRestoresTransferFieldsAndSavesEditedNote() {
        var closed = false
        var saved: Transaction? = null
        val detail = TransactionDetail(
            transaction = Transaction(
                id = 42L, type = TransactionType.TRANSFER, amount = 12345L,
                accountId = 1L, toAccountId = 2L, categoryId = 1L,
                transactionTime = LocalDateTime.of(2026, 11, 7, 20, 59, 21)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                note = "账户间划转",
            ),
            account = Account(id = 1L, name = "银行卡", type = AccountType.DEBIT),
            toAccount = Account(id = 2L, name = "零钱", type = AccountType.WALLET),
            category = CategoryWithIcon(
                Category(id = 1L, name = "账户互转", type = CategoryType.TRANSFER, iconId = 1L),
                Icon(id = 1L, keyName = "transfer", displayName = "转账", iconUri = "res://"),
            ),
        )
        compose.setContent {
            MaterialTheme {
                TransactionDetailsContent(
                    TransactionDetailsUiState.Success(
                        detail, listOf(detail.category), listOfNotNull(detail.account, detail.toAccount).map { AccountWithTemplate(it) },
                    ),
                    onBack = { closed = true },
                    onSave = { saved = it },
                )
            }
        }
        compose.onNodeWithText("交易详情").assertIsDisplayed()
        compose.onNodeWithText("123.45").assertIsDisplayed()
        compose.onNodeWithText("转账").assertIsSelected().assertIsEnabled()
        compose.onNodeWithText("账户互转").assertIsDisplayed()
        compose.onNodeWithText("账户间划转").assertIsDisplayed()
        compose.onNodeWithText("转出：银行卡").assertIsDisplayed()
        compose.onNodeWithText("转入：零钱").assertIsDisplayed()
        compose.onNodeWithContentDescription("备注").performTextReplacement("更新后的备注").performImeAction()
        compose.onNodeWithText("完成").performClick()
        assertEquals(detail.transaction.copy(note = "更新后的备注"), saved)
        compose.onNodeWithContentDescription("关闭交易详情").performClick()
        assertTrue(closed)
    }
}
