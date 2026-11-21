package cn.turingzy.ledger.ui.feature.bill.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.TransactionBatchChange
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.BaseAlertDialog
import cn.turingzy.ledger.ui.component.base.IconButtonBase
import cn.turingzy.ledger.ui.component.base.DividerBase
import cn.turingzy.ledger.ui.component.base.EmptyStateBase
import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.biz.TypeIconBiz
import cn.turingzy.ledger.ui.component.biz.categoryGlyphBiz
import cn.turingzy.ledger.ui.component.input.SegmentedControl
import cn.turingzy.ledger.ui.component.navigation.BottomBarContentPadding
import cn.turingzy.ledger.ui.component.navigation.BottomBarHeight
import cn.turingzy.ledger.ui.component.navigation.BottomBarVerticalPadding
import cn.turingzy.ledger.ui.feature.bill.sheet.RecordAccountSheet
import cn.turingzy.ledger.ui.feature.bill.sheet.categoryType
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.rememberUiIndication
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle

internal val BillSelectionBarPadding = BottomBarContentPadding

@Composable
internal fun BillSelectionBar(
    selectedCount: Int,
    allSelected: Boolean,
    enabled: Boolean,
    onSelectAll: (Boolean) -> Unit,
    onSelectAccount: () -> Unit,
    onSelectCategory: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerStyle.colors
    val actionsEnabled = enabled && selectedCount > 0
    SurfaceBase(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = BottomBarVerticalPadding),
        shape = CircleShape, color = colors.surface, shadowElevation = 2.dp,
        border = BorderStroke(0.5.dp, colors.divider),
    ) {
        Row(
            Modifier.fillMaxWidth().height(BottomBarHeight).padding(horizontal = 8.dp).testTag("billSelectionBar"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.clip(CircleShape)
                    .toggleable(
                        value = allSelected, role = Role.Checkbox, enabled = enabled,
                        interactionSource = null, indication = rememberUiIndication(), onValueChange = onSelectAll,
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
            ) {
                BillSelectionIndicator(allSelected)
                Text(stringResource(R.string.bill_select_all), style = LedgerStyle.typography.bodySmall, maxLines = 1)
                Text(selectedCount.toString(), style = LedgerStyle.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            BillSelectionAction(stringResource(R.string.ui_account), colors.textPrimary, actionsEnabled, onSelectAccount)
            BillSelectionAction(stringResource(R.string.ui_category), colors.textPrimary, actionsEnabled, onSelectCategory)
            BillSelectionAction(stringResource(R.string.bill_delete_action), colors.expense, actionsEnabled, onDelete)
        }
    }
}

@Composable
private fun BillSelectionAction(label: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) tint else LedgerStyle.colors.textSecondary.copy(alpha = LedgerStyle.opacity.disabled)
    Text(
        label,
        modifier = Modifier.clip(CircleShape).uiClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        style = LedgerStyle.typography.bodySmall,
        color = color,
        maxLines = 1,
    )
}

internal enum class BillActionPicker {
    Account, Category;

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Sheet(
        accounts: List<AccountWithTemplate>,
        categories: List<CategoryWithIcon>,
        accountCount: (Long) -> Int,
        categoryCount: (Long) -> Int,
        selectedCount: Int,
        type: TransactionType,
        onDismiss: () -> Unit,
        onSelect: (TransactionBatchChange) -> Unit,
    ) {
        val height = LocalConfiguration.current.screenHeightDp.dp * 0.55f
        var selectedId by rememberSaveable(this) { mutableStateOf<Long?>(null) }
        var categoryType by rememberSaveable {
            mutableStateOf(if (type == TransactionType.TRANSFER) TransactionType.EXPENSE else type)
        }
        val visibleCategories = categories.filter { it.category.type == categoryType.categoryType() }
        val confirmedChange = selectedId?.let { id ->
            when (this) {
                Account -> TransactionBatchChange.Account(id).takeIf { accounts.any { it.account.id == id } }
                Category -> TransactionBatchChange.Category(id).takeIf { visibleCategories.any { it.category.id == id } }
            }
        }
        val confirm = confirmedChange?.let { change -> { onSelect(change) } }
        if (this == Account) {
            val title = stringResource(R.string.ui_select_account)
            RecordAccountSheet(
                title = title,
                accounts = accounts,
                selectedId = selectedId ?: 0L,
                transactionType = type,
                height = height,
                onDismiss = onDismiss,
                onSelect = { selectedId = it },
                emptyMessage = stringResource(R.string.bill_no_compatible_accounts),
                optionHint = { id -> batchOptionHint(accountCount(id), selectedCount) },
                selectionColor = LedgerStyle.colors.info,
                selectionCheckOnNameRow = true,
                header = {
                    BillPickerHeader(stringResource(R.string.ui_close_account_picker), onDismiss, confirm) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
            )
        } else ModalBottomSheetBase(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            contentHandlesInsets = true,
        ) {
            Column(Modifier.fillMaxWidth().height(height).navigationBarsPadding()) {
                val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                BillPickerHeader(stringResource(R.string.bill_close_category_picker), onDismiss, confirm) {
                    SegmentedControl(
                        labels = listOf(stringResource(R.string.ui_expense), stringResource(R.string.ui_income)),
                        selectedIndex = types.indexOf(categoryType),
                        onSelect = { index ->
                            val next = types[index]
                            if (categoryType != next) {
                                categoryType = next
                                selectedId = null
                            }
                        },
                        modifier = Modifier.width(168.dp),
                        height = 28.dp,
                        fontSize = LedgerStyle.typography.label.fontSize,
                    )
                }
                DividerBase()
                if (visibleCategories.isEmpty()) EmptyStateBase(title = stringResource(R.string.bill_no_compatible_categories))
                else key(categoryType) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 64.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall),
                        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                    ) {
                        items(visibleCategories, key = { it.category.id }) { item ->
                            val categoryId = item.category.id
                            val isSelected = selectedId == categoryId
                            Column(
                                Modifier.selectable(
                                    selected = isSelected,
                                    interactionSource = null,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = rememberUiAction({ selectedId = categoryId }),
                                )
                                    .padding(horizontal = 2.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny),
                            ) {
                                TypeIconBiz(item.icon.categoryGlyphBiz(), categoryType, highlighted = isSelected)
                                Text(
                                    item.category.name, style = LedgerStyle.typography.label,
                                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    stringResource(R.string.bill_partial_batch_hint, categoryCount(categoryId), selectedCount),
                                    style = LedgerStyle.typography.caption, color = LedgerStyle.colors.textSecondary,
                                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillPickerHeader(
    closeDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = LedgerStyle.dimensions.touchTarget).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonBase(MaterialSymbols.Close, closeDescription, onDismiss, showPressFeedback = false)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { content() }
        IconButtonBase(
            MaterialSymbols.Check, stringResource(R.string.ui_confirm), { onConfirm?.invoke() },
            enabled = onConfirm != null, contentColor = LedgerStyle.colors.success, showPressFeedback = false,
        )
    }
}

@Composable
private fun batchOptionHint(compatible: Int, selected: Int): String? =
    if (compatible < selected) stringResource(R.string.bill_partial_batch_hint, compatible, selected) else null

@Composable
internal fun BillDeleteDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    BaseAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(R.plurals.bill_delete_title, count, count)) },
        text = { Text(stringResource(R.string.ui_delete_transaction_message)) },
        onConfirm = onConfirm,
        destructive = true,
        confirmLabel = stringResource(R.string.bill_delete_action),
    )
}
