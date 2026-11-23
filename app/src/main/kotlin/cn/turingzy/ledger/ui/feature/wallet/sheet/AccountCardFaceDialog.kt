package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.base.DialogWindowBase
import cn.turingzy.ledger.ui.component.base.DialogActions
import cn.turingzy.ledger.ui.component.base.dialogContentPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.BuiltInCardFace
import cn.turingzy.ledger.model.account.selectableCardTemplates
import cn.turingzy.ledger.ui.component.card.bank.rememberCircularCardPagerState
import cn.turingzy.ledger.ui.component.card.bank.CardPageIndicator
import cn.turingzy.ledger.ui.component.card.bank.TitaniumBankCard
import cn.turingzy.ledger.ui.component.card.bank.titaniumTitle

/**
 * 账户卡面设计选择弹窗。
 *
 * 允许用户通过左右滑动预览并挑选内置或自定义的银行卡卡面样式。
 *
 * @param templates 可用的卡面模板列表
 * @param selectedId 当前已选中的模板 ID
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 确认选择卡面回调，传回所选模板 ID
 */
@Composable
internal fun AccountCardFaceDialog(
    templates: List<CardTemplate>,
    selectedId: Long?,
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
    DialogWindowBase(onDismissRequest = onDismiss) {
        SurfaceBase(
            shape = LedgerStyle.shapes.dialog,
            color = LedgerStyle.colors.overlay, contentColor = LedgerStyle.colors.textPrimary,
        ) {
            Column(Modifier.dialogContentPadding(LedgerStyle.spacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_select_card_design), style = MaterialTheme.typography.titleMedium)
                HorizontalPager(state = pager, key = { it }, pageSpacing = 12.dp) { page ->
                    val face = faces[Math.floorMod(page, faces.size)]
                    TitaniumBankCard(
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
                DialogActions(
                    onCancel = onDismiss,
                    onConfirm = { onConfirm(faces[Math.floorMod(pager.settledPage, faces.size)].id) },
                    confirmLabel = textResources.getString(cn.turingzy.ledger.R.string.ui_apply_card_design),
                    confirmEnabled = !pager.isScrollInProgress,
                )
            }
        }
    }
}
