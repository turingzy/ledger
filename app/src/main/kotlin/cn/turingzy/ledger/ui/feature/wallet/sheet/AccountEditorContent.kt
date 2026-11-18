package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.feature.wallet.component.displayName
import cn.turingzy.ledger.ui.component.base.BaseButtonVariant
import cn.turingzy.ledger.ui.component.base.BaseActionButton
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.input.LedgerFormDropdown as AccountFormDropdown
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
internal fun AccountEditorContent(
    account: Account?,
    templates: List<CardTemplate>,
    onSave: (Account) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var name by rememberSaveable(account?.id) { mutableStateOf(account?.name.orEmpty()) }
    var type by rememberSaveable(account?.id) { mutableStateOf(account?.type ?: AccountType.DEBIT) }
    var balance by rememberSaveable(account?.id) { mutableStateOf(account?.initialBalance?.toFormattedAmount().orEmpty()) }
    var cardNumber by rememberSaveable(account?.id) { mutableStateOf(account?.cardNumber.orEmpty()) }
    val primaryCurrency = cn.turingzy.ledger.ui.component.biz.LocalLedgerCurrency.current
    var currency by rememberSaveable(account?.id) { mutableStateOf(account?.currency ?: primaryCurrency) }
    var templateId by rememberSaveable(account?.id) { mutableStateOf(account?.templateId) }
    var includeInTotal by rememberSaveable(account?.id) { mutableStateOf(account?.includeInTotal ?: true) }
    var showCardFaceDialog by rememberSaveable(account?.id) { mutableStateOf(false) }
    val input = AccountFormInput(name, type, balance, cardNumber, currency, templateId, includeInTotal)
    val result = if (account == null) input.toAccountOrNull(templates) else input.toUpdatedAccountOrNull(account, templates)
    val activeTemplates = remember(templates) { templates.filterNot { it.isDeleted } }
    val currencies = accountCurrencyOptions
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme

    AccountSheetContent(
        title = if (account == null) textResources.getString(cn.turingzy.ledger.R.string.ui_add_account) else textResources.getString(cn.turingzy.ledger.R.string.ui_account_details),
        saveEnabled = result != null,
        onSave = {
            val value = result ?: return@AccountSheetContent
            focusManager.clearFocus()
            onSave(value)
        },
        modifier = modifier,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_include_in_total_assets), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Switch(
                    checked = includeInTotal,
                    onCheckedChange = { includeInTotal = it },
                    modifier = Modifier.scale(0.72f).semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_include_in_total_assets) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.primary,
                        checkedThumbColor = colors.surface,
                        checkedBorderColor = colors.primary,
                    ),
                )
            }
        }
        AccountFormTextField(
            label = textResources.getString(cn.turingzy.ledger.R.string.ui_account_name), value = name, onValueChange = { name = it },
            placeholder = textResources.getString(cn.turingzy.ledger.R.string.ui_salary_account),
        )
        AccountFormDropdown(
            label = textResources.getString(cn.turingzy.ledger.R.string.ui_account_type), value = type.displayName(textResources), options = AccountType.entries,
            optionLabel = { it.displayName(textResources) }, onSelect = { type = it },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
            AccountFormTextField(
                label = if (type == AccountType.CREDIT) textResources.getString(cn.turingzy.ledger.R.string.ui_opening_debt) else textResources.getString(cn.turingzy.ledger.R.string.ui_opening_balance),
                value = balance, onValueChange = { balance = it }, placeholder = "0.00",
                error = if (input.balanceCents == null) textResources.getString(cn.turingzy.ledger.R.string.ui_enter_a_valid_amount) else null,
                readOnly = account != null, keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1.2f),
            )
            if (account == null) {
                AccountFormDropdown(
                    label = textResources.getString(cn.turingzy.ledger.R.string.ui_currency), value = currency, options = currencies,
                    optionLabel = { it }, onSelect = { currency = it }, modifier = Modifier.weight(0.8f),
                    alignMenuToStart = false,
                )
            } else {
                AccountFormTextField(
                    label = textResources.getString(cn.turingzy.ledger.R.string.ui_currency), value = account.currency, onValueChange = {}, readOnly = true,
                    modifier = Modifier.weight(0.8f),
                )
            }
        }
        if (account != null) {
            AccountFormTextField(
                label = if (account.type == AccountType.CREDIT) textResources.getString(cn.turingzy.ledger.R.string.ui_current_debt) else textResources.getString(cn.turingzy.ledger.R.string.ui_current_balance),
                value = account.currentBalance.toFormattedAmount(useGrouping = true),
                onValueChange = {}, suffix = account.currency, readOnly = true,
            )
        }
        if (input.requiresCardTail) {
            AccountTailNumberField(value = cardNumber.takeLast(4), onValueChange = { cardNumber = it })
        }
        AccountCardFaceField(
            value = if (templateId == null) textResources.getString(cn.turingzy.ledger.R.string.ui_default_card_design) else activeTemplates.firstOrNull { it.id == templateId }?.name ?: textResources.getString(cn.turingzy.ledger.R.string.ui_select_a_card_design_again),
            onClick = { focusManager.clearFocus(); showCardFaceDialog = true },
            enabled = input.canSelectCardFace,
            disabledHint = if (input.requiresCardTail) textResources.getString(cn.turingzy.ledger.R.string.ui_enter_an_account_name_and_last_four_digits) else textResources.getString(cn.turingzy.ledger.R.string.ui_enter_an_account_name_first),
        )

        if (account != null && onDelete != null) {
            BaseActionButton(variant = BaseButtonVariant.Outlined,
                onClick = { focusManager.clearFocus(); onDelete() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp),
                shape = LedgerStyle.shapes.field,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.3f)),
            ) {
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_delete_account), style = MaterialTheme.typography.bodyMedium)
            }
        }

    }
    if (showCardFaceDialog && input.canSelectCardFace) {
        AccountCardFaceDialog(
            templates = activeTemplates, selectedId = templateId, accountName = name,
            accountType = type, tailNumber = cardNumber,
            onDismiss = { showCardFaceDialog = false },
            onConfirm = { templateId = it; showCardFaceDialog = false },
        )
    }
}
