package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.card.bank.titaniumTitle
import cn.turingzy.ledger.ui.feature.wallet.component.displayName
import cn.turingzy.ledger.ui.component.base.ButtonVariantBase
import cn.turingzy.ledger.ui.component.base.ActionButtonBase
import cn.turingzy.ledger.ui.component.input.FormTextField
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.biz.currencyOptionsBiz
import cn.turingzy.ledger.ui.component.input.FormDropdown as AccountFormDropdown
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.resolveBuiltInAppearance
import cn.turingzy.ledger.util.toFormattedAmount

/**
 * 账户表单编辑器核心内容组件。
 *
 * 统一处理账户的新建与编辑流程，包含名称、类型、期初余额/负债、币种、卡号尾号、卡面样式及计入总资产开关等字段的输入与校验。
 *
 * @param account 待编辑的账户；为 null 时表示处于新增账户模式
 * @param templates 可用的卡面模板列表
 * @param onSave 校验通过后的保存回调，传回构建或更新后的 [Account] 对象
 * @param modifier 外部修饰符
 * @param onDelete 删除账户回调（编辑已有账户时有效）
 * @param onDismiss 取消或关闭回调
 * @param assignedTemplate 当前账户初始关联的模板
 */
@Composable
internal fun AccountEditorContent(
    account: Account?,
    templates: List<CardTemplate>,
    onSave: (Account) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    assignedTemplate: CardTemplate? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalResources.current
    var name by rememberSaveable(account?.id) { mutableStateOf(account?.name.orEmpty()) }
    var type by rememberSaveable(account?.id) { mutableStateOf(account?.type ?: AccountType.DEBIT) }
    var balance by rememberSaveable(account?.id) { mutableStateOf(account?.initialBalance?.toFormattedAmount().orEmpty()) }
    var cardNumber by rememberSaveable(account?.id) { mutableStateOf(account?.cardNumber.orEmpty()) }
    val primaryCurrency = cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz.current
    var currency by rememberSaveable(account?.id) { mutableStateOf(account?.currency ?: primaryCurrency) }
    var templateId by rememberSaveable(account?.id) { mutableStateOf(account?.templateId) }
    var includeInTotal by rememberSaveable(account?.id) { mutableStateOf(account?.includeInTotal ?: true) }
    var showCardFaceDialog by rememberSaveable(account?.id) { mutableStateOf(false) }
    val input = AccountFormInput(name, type, balance, cardNumber, currency, templateId, includeInTotal)
    val result = if (account == null) input.toAccountOrNull(templates) else input.toUpdatedAccountOrNull(account, templates)
    val activeTemplates = remember(templates) { templates.filterNot { it.isDeleted } }
    val selectedTemplate = activeTemplates.firstOrNull { it.id == templateId }
        ?: assignedTemplate?.takeIf { it.id == templateId }
    val cardFaceName = if (account == null && templateId == null) {
        textResources.getString(cn.turingzy.ledger.R.string.ui_choose_card_design)
    } else selectedTemplate.resolveBuiltInAppearance(activeTemplates).titaniumTitle()
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme

    AccountSheetContent(
        title = if (account == null) textResources.getString(cn.turingzy.ledger.R.string.ui_add_account) else textResources.getString(cn.turingzy.ledger.R.string.ui_account_details),
        saveEnabled = result != null,
        saveInFooter = account == null,
        onDismiss = { focusManager.clearFocus(); onDismiss() },
        onSave = {
            val value = result ?: return@AccountSheetContent
            focusManager.clearFocus()
            onSave(value)
        },
        modifier = modifier,
    ) {
        val yesLabel = textResources.getString(R.string.ui_yes)
        val noLabel = textResources.getString(R.string.ui_no)
        AccountFormDropdown(
            label = textResources.getString(R.string.ui_include_in_total_assets),
            value = if (includeInTotal) yesLabel else noLabel,
            options = listOf(true, false),
            optionLabel = { if (it) yesLabel else noLabel },
            isSelected = { it == includeInTotal },
            onSelect = { includeInTotal = it },
        )
        FormTextField(
            label = textResources.getString(cn.turingzy.ledger.R.string.ui_account_name), value = name, onValueChange = { name = it },
            placeholder = textResources.getString(cn.turingzy.ledger.R.string.ui_salary_account),
        )
        AccountFormDropdown(
            label = textResources.getString(cn.turingzy.ledger.R.string.ui_account_type), value = type.displayName(textResources), options = AccountType.entries,
            optionLabel = { it.displayName(textResources) }, onSelect = { type = it },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
            FormTextField(
                label = if (type == AccountType.CREDIT) textResources.getString(cn.turingzy.ledger.R.string.ui_opening_debt) else textResources.getString(cn.turingzy.ledger.R.string.ui_opening_balance),
                value = balance, onValueChange = { balance = it }, placeholder = "0.00",
                error = if (input.balanceCents == null) textResources.getString(cn.turingzy.ledger.R.string.ui_enter_a_valid_amount) else null,
                readOnly = account != null, keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1.2f),
            )
            if (account == null) {
                AccountFormDropdown(
                    label = textResources.getString(cn.turingzy.ledger.R.string.ui_currency), value = currency,
                    options = currencyOptionsBiz(accountCurrencyOptions),
                    optionLabel = { it.second }, isSelected = { it.first == currency },
                    onSelect = { currency = it.first }, modifier = Modifier.weight(0.8f),
                    menuWidth = LedgerStyle.dimensions.currencyDropdownWidth,
                    alignMenuToStart = false,
                )
            } else {
                FormTextField(
                    label = textResources.getString(cn.turingzy.ledger.R.string.ui_currency), value = account.currency, onValueChange = {}, readOnly = true,
                    modifier = Modifier.weight(0.8f),
                )
            }
        }
        if (account != null) {
            FormTextField(
                label = if (account.type == AccountType.CREDIT) textResources.getString(cn.turingzy.ledger.R.string.ui_current_debt) else textResources.getString(cn.turingzy.ledger.R.string.ui_current_balance),
                value = account.currentBalance.toFormattedAmount(useGrouping = true),
                onValueChange = {}, suffix = account.currency, readOnly = true,
            )
        }
        if (input.requiresCardTail) {
            AccountTailNumberField(value = cardNumber.takeLast(4), onValueChange = { cardNumber = it })
        }
        AccountCardFaceField(
            value = cardFaceName,
            onClick = { focusManager.clearFocus(); showCardFaceDialog = true },
            enabled = input.canSelectCardFace,
            disabledHint = if (input.requiresCardTail) textResources.getString(cn.turingzy.ledger.R.string.ui_enter_an_account_name_and_last_four_digits) else textResources.getString(cn.turingzy.ledger.R.string.ui_enter_an_account_name_first),
        )

        if (account != null && onDelete != null) {
            ActionButtonBase(variant = ButtonVariantBase.Outlined,
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
            templates = activeTemplates, selectedId = templateId,
            onDismiss = { showCardFaceDialog = false },
            onConfirm = {
                templateId = it
                showCardFaceDialog = false
            },
        )
    }
}
