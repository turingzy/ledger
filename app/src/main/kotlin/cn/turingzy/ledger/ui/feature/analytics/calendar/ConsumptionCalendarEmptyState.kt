package cn.turingzy.ledger.ui.feature.analytics.calendar

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
import cn.turingzy.ledger.ui.component.base.ButtonBase
import cn.turingzy.ledger.ui.component.base.ButtonToneBase
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.EmptyStateBase
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle

@Composable
internal fun ConsumptionCalendarEmptyState(onRecord: () -> Unit, modifier: Modifier = Modifier) {
    CardBase(
        modifier = modifier,
        shape = LedgerStyle.shapes.summaryCard,
        contentPadding = PaddingValues(LedgerStyle.spacing.none),
    ) {
        EmptyStateBase(
            title = stringResource(R.string.ui_no_expenses_on_this_day),
            message = stringResource(R.string.calendar_empty_supporting),
            illustration = {
                Image(
                    painterResource(R.drawable.illustration_calendar_empty), contentDescription = null,
                    modifier = Modifier.size(LedgerStyle.dimensions.calendarEmptyIllustrationWidth, LedgerStyle.dimensions.calendarEmptyIllustrationHeight),
                )
            },
            actions = {
                ButtonBase(
                    stringResource(R.string.ui_new_transaction), onRecord,
                    modifier = Modifier.height(LedgerStyle.dimensions.compactButtonHeight),
                    tone = ButtonToneBase.Neutral, trailingIcon = MaterialSymbols.EditSquare,
                    iconSize = LedgerStyle.icons.small, shape = LedgerStyle.shapes.pill,
                    interaction = UiInteractionKind.Navigation,
                    showPressFeedback = false,
                )
            },
        )
    }
}
