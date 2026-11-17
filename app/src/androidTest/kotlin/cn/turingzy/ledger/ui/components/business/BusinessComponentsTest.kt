package cn.turingzy.ledger.ui.components.business

import cn.turingzy.ledger.ui.components.business.BusinessTransactionRow
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import cn.turingzy.ledger.model.TransactionDetail
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.components.business.preview.TransactionPreviewProvider
import cn.turingzy.ledger.ui.theme.LedgerTheme
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.LocalLedgerCurrency
import cn.turingzy.ledger.ui.component.MaterialSymbols
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BusinessComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun amountUsesTheSameSignAndCurrencyRulesAcrossContexts() {
        compose.setContent {
            LedgerTheme {
                Column {
                    BusinessAmountText(1234L, "CNY", type = TransactionType.EXPENSE, primaryCurrency = "CNY")
                    BusinessAmountText(1234L, "USD", type = TransactionType.INCOME, primaryCurrency = "CNY")
                    BusinessAmountText(null, "USD", primaryCurrency = "CNY")
                }
            }
        }
        listOf("−¥12.34", "+12.34 USD", "—").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun activeTransactionForwardsClicksButSoftDeletedTransactionHasNoRow() {
        val detail = TransactionPreviewProvider().values.first().detail
        var clicks = 0
        compose.setContent {
            LedgerTheme {
                Column {
                    TransactionFixture(detail, Modifier.testTag("active"), onClick = { clicks++ })
                    TransactionFixture(
                        detail.copy(transaction = detail.transaction.copy(isDeleted = true)),
                        Modifier.testTag("deleted"), onClick = { clicks++ },
                    )
                }
            }
        }
        compose.onNodeWithTag("deleted").assertDoesNotExist()
        compose.onNodeWithTag("active").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun longAmountLeavesSpaceForTheTransactionHeadline() {
        val detail = TransactionPreviewProvider().values.first().detail
        compose.setContent {
            LedgerTheme {
                TransactionFixture(detail.copy(transaction = detail.transaction.copy(amount = Long.MIN_VALUE)))
            }
        }
        compose.onNodeWithText(detail.category.category.name).assertIsDisplayed()
    }

    @Test
    fun ledgerAndCalendarRowsShareTheSameMutedCurrencySuffix() {
        val category = TransactionPreviewProvider().values.first().detail.category
        var unitColor = Color.Unspecified
        compose.setContent {
            CompositionLocalProvider(LocalLedgerCurrency provides "CNY") {
                LedgerTheme {
                    unitColor = LedgerStyle.colors.currencyUnit
                    Column {
                        BusinessTransactionRow(category.category.name, "银行卡", "12:00", 1234L, TransactionType.EXPENSE,
                            MaterialSymbols.Restaurant, currency = "USD")
                        BusinessTransactionItem(amount = 1234L, currency = "USD", type = TransactionType.EXPENSE,
                            category = category, accountName = "银行卡", timeText = "12:00")
                    }
                }
            }
        }
        val nodes = compose.onAllNodesWithText("−12.34 USD").fetchSemanticsNodes()
        assertEquals(2, nodes.size)
        val labels = nodes.map { it.config[SemanticsProperties.Text].single() }
        assertEquals(labels.first(), labels.last())
        val unit = labels.first().spanStyles.single()
        assertEquals(" USD", labels.first().text.substring(unit.start, unit.end))
        assertEquals(unitColor, unit.item.color)
        assertEquals(11.sp, unit.item.fontSize)
    }
    @Test
    fun transferDoesNotInventAMissingReceivedAmount() {
        compose.setContent { LedgerTheme { Column {
            for (received in listOf(null, 1700L)) BusinessTransactionItem(
                amount = 12345L, currency = "CNY", type = TransactionType.TRANSFER,
                category = null, accountName = "来源账户", timeText = "12:00", primaryCurrency = "CNY",
                destinationName = "目标账户", destinationCurrency = "USD", receivedAmount = received,
            )
        } } }
        compose.onNodeWithText("到账 —").assertIsDisplayed()
        compose.onNodeWithText("到账 17.00 USD").assertIsDisplayed()
    }

    @Composable
    private fun TransactionFixture(detail: TransactionDetail, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
        BusinessTransactionItem(
            detail.transaction.amount, detail.account.currency, detail.transaction.type,
            detail.category, detail.account.name, "00:00", modifier,
            isDeleted = detail.transaction.isDeleted, onClick = onClick,
        )
    }

}
