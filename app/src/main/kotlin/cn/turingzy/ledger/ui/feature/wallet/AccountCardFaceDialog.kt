package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.components.base.BaseSurface
import cn.turingzy.ledger.ui.components.base.BaseDialogWindow
import cn.turingzy.ledger.ui.component.accountCardColors
import cn.turingzy.ledger.ui.component.AccountCardBackground
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.ui.component.BankCardAspectRatio
import cn.turingzy.ledger.ui.component.rememberCircularCardPagerState
import cn.turingzy.ledger.ui.component.CardPageIndicator
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.SummaryCardIconButton

@Composable
internal fun AccountCardFaceDialog(
    templates: List<CardTemplate>,
    selectedId: Long?,
    accountName: String,
    accountType: AccountType,
    tailNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val faces = remember(templates) { listOf<CardTemplate?>(null) + templates.filterNot { it.isDeleted } }
    val pager = rememberCircularCardPagerState(faces.map { it?.id }, selectedId)
    val colors = MaterialTheme.colorScheme
    BaseDialogWindow(onDismissRequest = onDismiss) {
        BaseSurface(shape = LedgerStyle.shapes.dialog, color = colors.surface) {
            Column(Modifier.padding(LedgerStyle.spacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
                Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                    SummaryCardIconButton(
                        MaterialSymbols.Close, textResources.getString(cn.turingzy.ledger.R.string.ui_close_card_design_picker), onDismiss,
                        Modifier.align(Alignment.CenterStart), containerColor = colors.surface, contentColor = colors.onSurface,
                    )
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_select_card_design), style = MaterialTheme.typography.titleMedium)
                    AccountSaveButton(
                        enabled = !pager.isScrollInProgress,
                        onClick = { onConfirm(faces[Math.floorMod(pager.settledPage, faces.size)]?.id) },
                        modifier = Modifier.align(Alignment.CenterEnd),
                        contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_apply_card_design),
                    )
                }
                HorizontalPager(state = pager, key = { it }, pageSpacing = 12.dp) { page ->
                    val face = faces[Math.floorMod(page, faces.size)]
                    val account = Account(name = accountName, type = accountType, templateId = face?.id)
                    val textColor = face?.textColor?.cardFaceTextColor(colors.onPrimary) ?: colors.onPrimary
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(BankCardAspectRatio).clip(LedgerStyle.shapes.card)
                            .background(Brush.linearGradient(accountCardColors(account))),
                    ) {
                        AccountCardBackground(account, face, Modifier.fillMaxSize())
                        Text(
                            accountName.ifBlank { textResources.getString(cn.turingzy.ledger.R.string.ui_account_name) },
                            Modifier.align(Alignment.TopStart).padding(LedgerStyle.spacing.extraLarge),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (accountType == AccountType.DEBIT || accountType == AccountType.CREDIT) {
                            Text(
                                "•••• •••• •••• ${tailNumber.filter { it in '0'..'9' }.takeLast(4)}",
                                Modifier.align(Alignment.BottomStart).padding(LedgerStyle.spacing.extraLarge),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp, color = textColor,
                            )
                        }
                    }
                }
                CardPageIndicator(faces.size, Math.floorMod(pager.currentPage, faces.size))
                Text(
                    faces[Math.floorMod(pager.currentPage, faces.size)]?.name ?: textResources.getString(cn.turingzy.ledger.R.string.ui_default_card_design),
                    Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun String.cardFaceTextColor(fallback: Color): Color {
    val hex = removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return fallback
    val value = hex.toLongOrNull(16) ?: return fallback
    return Color((if (hex.length == 6) value or 0xFF000000L else value).toInt())
}
