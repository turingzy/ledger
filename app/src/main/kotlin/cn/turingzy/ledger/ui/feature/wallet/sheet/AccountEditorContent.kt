package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.base.primaryActionSheetHeight

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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
 * 统一处理账户的新建与编辑流程，包含账户属性、银行卡信息与卡面选择。
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
    var cardNumber by remember(account?.id) { mutableStateOf((account?.cardDetails?.number ?: account?.cardNumber).orEmpty()) }
    var cardExpiry by remember(account?.id) { mutableStateOf(account?.cardDetails?.expiry.orEmpty()) }
    var cardSecurityCode by remember(account?.id) { mutableStateOf(account?.cardDetails?.securityCode.orEmpty()) }
    val primaryCurrency = cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz.current
    var currency by rememberSaveable(account?.id) { mutableStateOf(account?.currency ?: primaryCurrency) }
    var templateId by rememberSaveable(account?.id) { mutableStateOf(account?.templateId) }
    var includeInTotal by rememberSaveable(account?.id) { mutableStateOf(account?.includeInTotal ?: true) }
    var showCardFaceDialog by rememberSaveable(account?.id) { mutableStateOf(false) }
    fun currentInput() = AccountFormInput(name, type, balance, cardNumber, currency, templateId, includeInTotal, cardExpiry, cardSecurityCode)
    val input = currentInput()
    val result = if (account == null) input.toAccountOrNull(templates) else input.toUpdatedAccountOrNull(account, templates)
    val activeTemplates = remember(templates) { templates.filterNot { it.isDeleted } }
    val selectedTemplate = activeTemplates.firstOrNull { it.id == templateId }
        ?: assignedTemplate?.takeIf { it.id == templateId }
    val cardFaceName = if (account == null && templateId == null) {
        textResources.getString(cn.turingzy.ledger.R.string.ui_choose_card_design)
    } else selectedTemplate.resolveBuiltInAppearance(activeTemplates).titaniumTitle()
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme

    val fields: @Composable ColumnScope.() -> Unit = {
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
        if (input.isBankCard) {
            FormTextField(
                label = textResources.getString(R.string.wallet_card_number),
                value = cardNumber,
                onValueChange = { cardNumber = it.filter { digit -> digit in '0'..'9' }.take(19) },
                error = textResources.getString(R.string.wallet_invalid_card_number).takeUnless { input.cardNumberValid },
                keyboardType = KeyboardType.Number,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
                FormTextField(
                    label = textResources.getString(R.string.wallet_card_expiry),
                    value = cardExpiry,
                    onValueChange = { raw ->
                        val digits = raw.filter { it in '0'..'9' }.take(4)
                        cardExpiry = if (digits.length > 2) digits.take(2) + "/" + digits.drop(2) else digits
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = "MM/YY",
                    keyboardType = KeyboardType.Number,
                    isError = !input.expiryValid,
                )
                FormTextField(
                    label = textResources.getString(R.string.wallet_card_security_code),
                    value = cardSecurityCode,
                    onValueChange = { cardSecurityCode = it.filter { digit -> digit in '0'..'9' }.take(4) },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.NumberPassword,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = !input.securityCodeValid,
                )
            }
        }
        AccountCardFaceField(
            value = cardFaceName,
            onClick = { focusManager.clearFocus(); showCardFaceDialog = true },
            enabled = input.canSelectCardFace,
            disabledHint = textResources.getString(R.string.ui_enter_an_account_name_first),
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
    AccountSheetContent(
        title = if (account == null) textResources.getString(cn.turingzy.ledger.R.string.ui_add_account) else textResources.getString(cn.turingzy.ledger.R.string.ui_account_details),
        sheetHeight = if (account == null) primaryActionSheetHeight() else LedgerStyle.dimensions.sheetMaxHeight,
        saveEnabled = result != null,
        saveInFooter = account == null,
        onDismiss = { focusManager.clearFocus(); onDismiss() },
        onSave = {
            val value = result ?: return@AccountSheetContent
            focusManager.clearFocus()
            onSave(value)
        },
        modifier = modifier,
        content = fields,
    )
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
