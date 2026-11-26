package cn.turingzy.ledger.ui.feature.wallet.component

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LifecycleResumeEffect
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.resolveBuiltInAppearance
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.ButtonBase
import cn.turingzy.ledger.ui.component.base.ButtonToneBase
import cn.turingzy.ledger.ui.component.base.ButtonVariantBase
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.DialogActions
import cn.turingzy.ledger.ui.component.base.DividerBase
import cn.turingzy.ledger.ui.component.base.DropdownIndicatorBase
import cn.turingzy.ledger.ui.component.base.IconButtonBase
import cn.turingzy.ledger.ui.component.biz.AmountTextBiz
import cn.turingzy.ledger.ui.component.card.bank.titaniumTitle
import cn.turingzy.ledger.ui.component.input.LedgerDropdown
import cn.turingzy.ledger.ui.component.input.DropdownDirection
import cn.turingzy.ledger.ui.component.input.FormInputBase
import cn.turingzy.ledger.ui.feature.wallet.sheet.AccountFormInput
import cn.turingzy.ledger.ui.feature.wallet.sheet.AccountCardFaceDialog
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle

/** 在轮播下直接展示和编辑账户属性，银行卡信息保留逐项复制。 */
@Composable
internal fun WalletAccountInfo(
    entry: AccountWithTemplate,
    templates: List<CardTemplate>,
    onUpdate: (Account) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val account = entry.account
    val details = account.cardDetails
    var input by remember(account.id) {
        mutableStateOf(AccountFormInput(
            name = account.name,
            type = account.type,
            currency = account.currency,
            cardNumber = (details?.number ?: account.cardNumber).orEmpty(),
            templateId = account.templateId,
            includeInTotal = account.includeInTotal,
            cardExpiry = details?.expiry.orEmpty(),
            cardSecurityCode = details?.securityCode.orEmpty(),
        ))
    }
    var confirmDelete by remember(account.id) { mutableStateOf(false) }
    var choosingFace by remember(account.id) { mutableStateOf(false) }
    var showCode by remember(account.id) { mutableStateOf(false) }
    LifecycleResumeEffect(account.id) {
        onPauseOrDispose { showCode = false }
    }
    val spacing = LedgerStyle.spacing
    val colors = LedgerStyle.colors
    val resources = LocalResources.current
    val focusManager = LocalFocusManager.current
    val template = templates.firstOrNull { !it.isDeleted && it.id == input.templateId }
        ?: entry.template?.takeIf { it.id == input.templateId }
    val yes = stringResource(R.string.ui_yes)
    val no = stringResource(R.string.ui_no)
    val credit = input.type == AccountType.CREDIT

    fun update(change: AccountFormInput.() -> AccountFormInput) {
        val next = input.change()
        if (next == input) return
        input = next
        next.toUpdatedAccountOrNull(account, templates)?.let(onUpdate)
    }

    CardBase(
        outlined = true,
        contentPadding = PaddingValues(horizontal = spacing.cardPadding, vertical = spacing.small),
        contentSpacing = spacing.none,
    ) {
        val nameLabel = stringResource(R.string.ui_account_name)
        AccountInfoRow(nameLabel) {
            AccountInfoText(nameLabel, input.name, { value -> update { copy(name = value) } }, isError = input.name.isBlank(), align = TextAlign.End)
        }
        AccountInfoChoice(
            stringResource(R.string.ui_account_type), input.type, AccountType.entries,
            labelOf = { it.displayName(resources) }, onSelect = { type -> update { copy(type = type) } },
        )
        AccountInfoRow(stringResource(R.string.ui_currency)) {
            Text(account.currency, style = LedgerStyle.typography.bodyMedium)
        }
        AccountInfoRow(stringResource(if (credit) R.string.ui_opening_debt else R.string.ui_opening_balance)) {
            AmountTextBiz(account.initialBalance, account.currency, useGrouping = true, textStyle = LedgerStyle.typography.bodyMedium)
        }
        AccountInfoRow(stringResource(if (credit) R.string.ui_current_debt else R.string.ui_current_balance)) {
            AmountTextBiz(account.currentBalance, account.currency, useGrouping = true, textStyle = LedgerStyle.typography.bodyMedium)
        }
        val cardFaceLabel = stringResource(R.string.ui_card_design)
        AccountInfoRow(cardFaceLabel, Modifier.accountInfoAction(cardFaceLabel) {
            focusManager.clearFocus()
            choosingFace = true
        }) {
            Text(template.resolveBuiltInAppearance(templates).titaniumTitle(), style = LedgerStyle.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(MaterialSymbols.ChevronRight, null, Modifier.size(LedgerStyle.icons.small), tint = colors.textSecondary)
        }
        AccountInfoChoice(
            stringResource(R.string.ui_include_in_total_assets), input.includeInTotal, listOf(true, false),
            labelOf = { if (it) yes else no }, onSelect = { include -> update { copy(includeInTotal = include) } },
        )
        if (input.isBankCard) {
            DividerBase()
            BankCardInfoRow(
                label = stringResource(R.string.wallet_card_number), value = input.cardNumber,
                onChange = { value -> update { copy(cardNumber = value.filter { it in '0'..'9' }.take(19)) } },
                copyValue = input.normalizedCardNumber?.takeIf { input.cardNumberValid && it.length >= 12 },
                isError = !input.cardNumberValid,
            )
            BankCardInfoRow(
                label = stringResource(R.string.wallet_card_expiry), value = input.cardExpiry,
                onChange = { value ->
                    val digits = value.filter { it in '0'..'9' }.take(4)
                    update { copy(cardExpiry = if (digits.length > 2) digits.take(2) + "/" + digits.drop(2) else digits) }
                },
                copyValue = input.cardExpiry.takeIf { it.isNotBlank() && input.expiryValid },
                placeholder = "MM/YY",
                isError = !input.expiryValid,
            )
            BankCardInfoRow(
                label = stringResource(R.string.wallet_card_security_code), value = input.cardSecurityCode,
                onChange = { value -> update { copy(cardSecurityCode = value.filter { it in '0'..'9' }.take(4)) } },
                copyValue = input.cardSecurityCode.takeIf { it.isNotBlank() && input.securityCodeValid },
                isError = !input.securityCodeValid, secret = true, revealed = showCode,
                action = if (input.cardSecurityCode.isBlank()) null else {
                    {
                        IconButtonBase(
                            if (showCode) MaterialSymbols.VisibilityOff else MaterialSymbols.Visibility,
                            stringResource(if (showCode) R.string.wallet_hide_security_code else R.string.wallet_show_security_code),
                            onClick = { showCode = !showCode }, iconSize = LedgerStyle.icons.small,
                        )
                    }
                },
            )
        }
        DividerBase()
        if (confirmDelete) {
            Text(
                stringResource(R.string.wallet_delete_account_confirmation, account.name),
                style = LedgerStyle.typography.bodySmall, color = colors.textSecondary,
            )
            DialogActions(
                onCancel = { confirmDelete = false },
                onConfirm = { confirmDelete = false; onDelete(account.id) },
                confirmLabel = stringResource(R.string.bill_delete_action), destructive = true,
            )
        } else {
            ButtonBase(
                stringResource(R.string.ui_delete_account), { confirmDelete = true },
                modifier = Modifier.align(Alignment.End), variant = ButtonVariantBase.Text,
                tone = ButtonToneBase.Destructive,
            )
        }
    }
    if (choosingFace) {
        AccountCardFaceDialog(
            templates, input.templateId,
            onDismiss = { choosingFace = false },
            onConfirm = { id ->
                update { copy(templateId = id) }
                choosingFace = false
            },
        )
    }
}

/** 同一行显示账户字段名和右侧值，使用列表间距而非表单边框。 */
@Composable
private fun AccountInfoRow(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = LedgerStyle.dimensions.compactIconButton),
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = LedgerStyle.typography.bodySmall, color = LedgerStyle.colors.textSecondary)
        value()
    }
}

/** 沿用应用的下拉菜单，锚点显示为可直接操作的详情行。 */
@Composable
private fun <T> AccountInfoChoice(
    label: String,
    value: T,
    options: List<T>,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    LedgerDropdown(options, labelOf, { it == value }, onSelect, direction = DropdownDirection.DOWN) { modifier, expanded, opensUp, toggle ->
        AccountInfoRow(label, modifier.accountInfoAction(label, toggle)) {
            Text(labelOf(value), style = LedgerStyle.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            DropdownIndicatorBase(expanded, opensUp = opensUp)
        }
    }
}

/** 限制详情行的按压背景，使它沿用表单控件的圆角。 */
@Composable
private fun Modifier.accountInfoAction(label: String, onClick: () -> Unit): Modifier =
    clip(LedgerStyle.shapes.field).uiClickable(onClick = onClick).semantics { contentDescription = label }

/** 在普通文字原位接受输入，使用文本颜色反馈校验状态。 */
@Composable
private fun AccountInfoText(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "—",
    isError: Boolean = false,
    align: TextAlign = TextAlign.Start,
    keyboardType: KeyboardType = KeyboardType.Text,
    transformation: VisualTransformation = VisualTransformation.None,
) {
    val style = LedgerStyle.typography.bodyMedium.copy(textAlign = align, fontFeatureSettings = "tnum")
    FormInputBase(
        value, onChange, style,
        Modifier.fillMaxWidth(if (align == TextAlign.End) .58f else 1f).semantics { contentDescription = label },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = transformation,
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Box(contentAlignment = if (align == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = style, color = LedgerStyle.colors.textSupporting)
                inner()
            }
        },
    )
}

/** 银行卡字段直接可编辑，复制时将内容标为敏感数据。 */
@Composable
private fun BankCardInfoRow(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    copyValue: String?,
    placeholder: String = "—",
    isError: Boolean = false,
    secret: Boolean = false,
    revealed: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val copied = stringResource(R.string.wallet_field_copied)
    Row(
        Modifier.fillMaxWidth().heightIn(min = LedgerStyle.dimensions.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = LedgerStyle.typography.caption, color = LedgerStyle.colors.textSecondary)
            AccountInfoText(
                label, value, onChange, placeholder, isError,
                keyboardType = if (secret) KeyboardType.NumberPassword else KeyboardType.Number,
                transformation = if (secret && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
            )
        }
        action?.invoke()
        if (copyValue != null) {
            IconButtonBase(
                MaterialSymbols.FileCopy, stringResource(R.string.wallet_copy_card_field, label),
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return@IconButtonBase
                    val clip = ClipData.newPlainText(label, copyValue)
                    clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
                    clipboard.setPrimaryClip(clip)
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                },
                iconSize = LedgerStyle.icons.small,
            )
        }
    }
}
