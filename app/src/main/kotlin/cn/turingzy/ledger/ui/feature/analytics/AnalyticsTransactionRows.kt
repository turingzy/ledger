package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.biz.TransactionRowBiz
import cn.turingzy.ledger.ui.component.biz.categoryGlyphBiz
import cn.turingzy.ledger.ui.feature.bill.BillTransactionActions
import cn.turingzy.ledger.ui.feature.bill.BillViewModel
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import cn.turingzy.ledger.ui.theme.LedgerStyle

/** Supplies the bill operations to charts that can open a transaction list. */
internal val LocalAnalyticsBillViewModel = staticCompositionLocalOf<BillViewModel?> { null }

internal typealias AnalyticsTransactionRow = @Composable (AnalyticsTransaction, Boolean, Boolean) -> Unit

/** Connects analytics projections to the same row and mutation flows as the ledger. */
@Composable
internal fun AnalyticsTransactionRows(
    transactions: List<AnalyticsTransaction>,
    timeText: (AnalyticsTransaction) -> String,
    viewModel: BillViewModel?,
    transactionViewModel: TransactionDetailsViewModel?,
    snackbarHostState: SnackbarHostState,
    onTransactionClick: ((Long) -> Unit)? = null,
    content: @Composable (AnalyticsTransactionRow) -> Unit,
) {
    val fallback: AnalyticsTransactionRow = { entry, roundTop, roundBottom ->
        val corners = LedgerStyle.shapes.summaryCard
        val square = CornerSize(LedgerStyle.spacing.none)
        val shape = corners.copy(
            topStart = if (roundTop) corners.topStart else square,
            topEnd = if (roundTop) corners.topEnd else square,
            bottomStart = if (roundBottom) corners.bottomStart else square,
            bottomEnd = if (roundBottom) corners.bottomEnd else square,
        )
        val category = entry.category
        val name = category?.category?.name ?: entry.note.ifBlank { stringResource(R.string.business_uncategorized) }
        Box(Modifier.clip(shape).background(LedgerStyle.colors.surface)) {
            TransactionRowBiz(
                categoryName = name,
                timeText = timeText(entry),
                note = entry.note.takeIf { category != null },
                amount = entry.amount,
                currency = entry.currency,
                type = entry.type,
                icon = category?.icon?.categoryGlyphBiz() ?: MaterialSymbols.Payments,
                onClick = onTransactionClick?.let { click -> { click(entry.id) } },
            )
        }
    }
    if (viewModel == null || transactionViewModel == null) {
        content(fallback)
        return
    }

    val entriesById = remember(transactions) { transactions.associateBy { it.id } }
    val details = remember(transactions) { transactions.mapNotNull { it.detail } }
    BillTransactionActions(
        dayGroups = emptyList(),
        ungroupedRows = details,
        viewModel = viewModel,
        transactionViewModel = transactionViewModel,
        snackbarHostState = snackbarHostState,
        timeText = { detail -> entriesById[detail.transaction.id]?.let(timeText).orEmpty() },
    ) { billRow ->
        content { entry, roundTop, roundBottom ->
            val detail = entry.detail
            if (detail == null) fallback(entry, roundTop, roundBottom)
            else billRow(detail, roundTop, roundBottom)
        }
    }
}
