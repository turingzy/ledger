package cn.turingzy.ledger.ui.feature.analytics

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.ui.component.LocalLedgerCurrency
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

private val previewDay = LocalDate.of(2026, 11, 14)

internal class ConsumptionCalendarPreviewProvider : PreviewParameterProvider<AnalyticsDashboardState> {
    override val values: Sequence<AnalyticsDashboardState> = sequence {
        yield(AnalyticsDashboardState(loading = false))
        yield(AnalyticsDashboardState(loading = true))
        yield(AnalyticsDashboardState(loading = false, amountsAvailable = false))
        val category = CategoryWithIcon(
            Category(id = 1L, name = "餐饮", type = CategoryType.EXPENSE, iconId = 1L),
            Icon(id = 1L, keyName = "ic_food", displayName = "餐饮", iconUri = ""),
        )
        val entry = AnalyticsTransaction(1L, previewDay, "午餐", "银行卡", 12345L, category = category, time = LocalTime.NOON)
        yield(AnalyticsDashboardState(days = mapOf(previewDay to AnalyticsDay(12345L, 0L)), transactions = listOf(entry), loading = false))
        yield(AnalyticsDashboardState(days = mapOf(previewDay to AnalyticsDay(7200L, 0L)), transactions = listOf(
            entry.copy(currency = "USD", amount = 1000L, primaryAmount = 7200L),
        ), loading = false))
        yield(AnalyticsDashboardState(days = mapOf(previewDay to AnalyticsDay(12345L, 0L)), transactions = listOf(
            entry.copy(note = "较长的交易备注，用于核对交易金额和内容列的布局", category = null),
        ), loading = false))
    }
}

@Preview(name = "Calendar Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Calendar Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConsumptionCalendarPreview(@PreviewParameter(ConsumptionCalendarPreviewProvider::class) state: AnalyticsDashboardState) {
    CompositionLocalProvider(LocalLedgerCurrency provides state.currency) {
        LedgerTheme {
            ConsumptionCalendarContent(state, YearMonth.from(previewDay), {}, today = previewDay)
        }
    }
}

@Preview(name = "Future Empty Light", showBackground = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Future Empty Dark", showBackground = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FutureConsumptionCalendarPreview() {
    LedgerTheme {
        ConsumptionCalendarContent(
            AnalyticsDashboardState(loading = false), YearMonth.from(previewDay).plusMonths(1), {}, today = previewDay,
        )
    }
}
