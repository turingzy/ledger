package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.components.base.BaseButton
import cn.turingzy.ledger.ui.components.base.BaseButtonTone
import cn.turingzy.ledger.ui.components.base.BaseCard
import cn.turingzy.ledger.ui.components.base.BaseEmptyState
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle

@Composable
internal fun ConsumptionCalendarEmptyState(onRecord: () -> Unit, modifier: Modifier = Modifier) {
    BaseCard(
        modifier = modifier,
        shape = LedgerStyle.shapes.summaryCard,
        contentPadding = PaddingValues(LedgerStyle.spacing.none),
    ) {
        BaseEmptyState(
            title = stringResource(R.string.ui_no_expenses_on_this_day),
            message = stringResource(R.string.calendar_empty_supporting),
            illustration = {
                Image(
                    painterResource(R.drawable.illustration_calendar_empty), contentDescription = null,
                    modifier = Modifier.size(LedgerStyle.dimensions.calendarEmptyIllustrationWidth, LedgerStyle.dimensions.calendarEmptyIllustrationHeight),
                )
            },
            actions = {
                BaseButton(
                    stringResource(R.string.ui_new_transaction), onRecord,
                    modifier = Modifier.height(LedgerStyle.dimensions.compactButtonHeight),
                    tone = BaseButtonTone.Neutral, trailingIcon = MaterialSymbols.EditSquare,
                    iconSize = LedgerStyle.icons.small, shape = LedgerStyle.shapes.pill,
                    interaction = UiInteractionKind.Navigation,
                    showPressFeedback = false,
                )
            },
        )
    }
}
