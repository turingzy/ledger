package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.BaseSurface
import cn.turingzy.ledger.ui.component.base.BaseDialogWindow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.BuiltInCardFace
import cn.turingzy.ledger.model.account.selectableCardTemplates
import cn.turingzy.ledger.ui.component.card.bank.rememberCircularCardPagerState
import cn.turingzy.ledger.ui.component.card.bank.CardPageIndicator
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.BaseIconButton
import cn.turingzy.ledger.ui.component.card.bank.titaniumTitle
import cn.turingzy.ledger.ui.feature.wallet.component.BankCardItem

@Composable
internal fun AccountCardFaceDialog(
    templates: List<CardTemplate>,
    selectedId: Long?,
    accountName: String,
    accountType: AccountType,
    tailNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalResources.current
    val faces = remember(templates) { templates.selectableCardTemplates() }
    if (faces.isEmpty()) return

    val selectedTemplate = templates.firstOrNull { it.id == selectedId }
    val legacyPreset = selectedTemplate?.let(BuiltInCardFace::forTemplate)
        ?.takeIf { it.isLegacyDefault(selectedTemplate) }
    val initialId = legacyPreset?.id ?: selectedId ?: BuiltInCardFace.IridescentLight.id
    val pager = rememberCircularCardPagerState(faces.map { it.id }, initialId)
    val colors = MaterialTheme.colorScheme
    BaseDialogWindow(onDismissRequest = onDismiss) {
        BaseSurface(shape = LedgerStyle.shapes.dialog, color = colors.surface) {
            Column(Modifier.padding(LedgerStyle.spacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_select_card_design), style = MaterialTheme.typography.titleMedium)
                HorizontalPager(state = pager, key = { it }, pageSpacing = 12.dp) { page ->
                    val face = faces[Math.floorMod(page, faces.size)]
                    BankCardItem(
                        account = Account(
                            name = accountName.ifBlank { textResources.getString(cn.turingzy.ledger.R.string.ui_account_name) },
                            type = accountType,
                            cardNumber = tailNumber,
                        ),
                        template = face,
                        modifier = Modifier.fillMaxWidth(),
                        motionEnabled = page == pager.settledPage,
                    )
                }
                CardPageIndicator(faces.size, Math.floorMod(pager.currentPage, faces.size))
                Text(
                    faces[Math.floorMod(pager.currentPage, faces.size)].titaniumTitle(),
                    Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BaseIconButton(MaterialSymbols.Close, textResources.getString(cn.turingzy.ledger.R.string.ui_close_card_design_picker),
                        onDismiss, showPressFeedback = false)
                    Spacer(Modifier.weight(1f))
                    BaseIconButton(
                        MaterialSymbols.Check, textResources.getString(cn.turingzy.ledger.R.string.ui_apply_card_design),
                        { onConfirm(faces[Math.floorMod(pager.settledPage, faces.size)].id) },
                        enabled = !pager.isScrollInProgress, contentColor = colors.tertiary, showPressFeedback = false,
                    )
                }
            }
        }
    }
}
