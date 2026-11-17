package cn.turingzy.ledger.ui.feature.settings.screen

import cn.turingzy.ledger.ui.feature.settings.component.SettingsActionRow
import cn.turingzy.ledger.ui.feature.settings.component.SettingsItemHeight
import cn.turingzy.ledger.ui.feature.settings.component.SettingsPageBottomPadding
import cn.turingzy.ledger.ui.feature.settings.component.SettingsPageContentTopPadding
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.component.base.BaseButtonVariant
import cn.turingzy.ledger.ui.component.base.BaseActionButton
import cn.turingzy.ledger.ui.component.base.BaseDropdownIndicator
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cn.turingzy.ledger.navigation.SettingsSubpageTransition
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.input.LedgerDropdown
import androidx.compose.foundation.layout.Box
import cn.turingzy.ledger.ui.component.base.BaseCard
import cn.turingzy.ledger.ui.component.base.BaseDivider
import cn.turingzy.ledger.ui.component.base.BaseIconButton
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon

internal typealias ParseRulePreview = cn.turingzy.ledger.model.transfer.BillParseRule

internal fun defaultParseRulePreviews(): List<ParseRulePreview> = ParseRulePreview.defaults()

private data class RulePage(val draft: ParseRulePreview?) {
    val depth: Int get() = if (draft != null) 1 else 0
}
private data class RuleOption(val id: Long?, val label: String)

@Composable
internal fun ParseRulesScreen(
    accounts: List<AccountWithTemplate>,
    categories: List<CategoryWithIcon>,
    rules: List<ParseRulePreview>,
    onSaveRule: (ParseRulePreview) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    initialDraft: ParseRulePreview? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var draft by remember { mutableStateOf(initialDraft) }
    var emptyDraft by remember { mutableStateOf(ParseRulePreview(id = 0L)) }
    val visibleDraft = draft ?: emptyDraft.takeIf { rules.isEmpty() }
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    KeepRuleEditorWindowStationary(enabled = visibleDraft != null)
    SettingsSubpageTransition(
        targetState = RulePage(visibleDraft),
        depth = { it.depth },
        contentKey = { it.depth },
    ) { page ->
        val editing = page.draft
        val existing = editing != null && rules.any { it.id == editing.id }
        val title = when {
            editing == null -> textResources.getString(cn.turingzy.ledger.R.string.ui_import_rules)
            existing -> textResources.getString(cn.turingzy.ledger.R.string.ui_rule_details)
            else -> textResources.getString(cn.turingzy.ledger.R.string.ui_new_rule)
        }

        SettingsSubpageScaffold(
            title = title,
            modifier = Modifier.testTag("parseRulePage"),
            onBack = if (editing != null) ({
                focusManager.clearFocus()
                if (rules.isEmpty()) onDismissRequest() else draft = null
            }) else onDismissRequest,
            actions = {
                BaseIconButton(
                    imageVector = if (editing == null) MaterialSymbols.Add else MaterialSymbols.Check,
                    contentDescription = if (editing == null) textResources.getString(cn.turingzy.ledger.R.string.ui_new_import_rule)
                        else textResources.getString(cn.turingzy.ledger.R.string.ui_save_rule),
                    showPressFeedback = false,
                    enabled = editing == null || editing.valid,
                    contentColor = if (editing == null) colors.onSurfaceVariant else colors.tertiary,
                    onClick = {
                        focusManager.clearFocus()
                        if (editing == null) {
                            draft = ParseRulePreview(id = (rules.maxOfOrNull { it.id } ?: 0L) + 1L)
                        } else {
                            onSaveRule(editing.copy(name = editing.name.trim()))
                            emptyDraft = ParseRulePreview(id = 0L)
                            draft = null
                        }
                    },
                )
            },
        ) {
            if (editing == null) {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(top = SettingsPageContentTopPadding, bottom = SettingsPageBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
                ) {
                    items(rules, key = { it.id }) { rule ->
                        SettingsActionRow(
                            title = rule.name,
                            subtitle = "从第 ${rule.startRow} 行读取",
                            onClick = { draft = rule },
                        )
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().settingsPageScroll().testTag("parseRuleForm"),
                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
                ) {
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_basic_information), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    RuleFieldGroup {
                        RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_rule_name), editing.name, textResources.getString(cn.turingzy.ledger.R.string.ui_enter_a_name), { draft = editing.copy(name = it) })
                        RuleDivider()
                        RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_starting_row), editing.startRow, textResources.getString(cn.turingzy.ledger.R.string.ui_first_data_row), { draft = editing.copy(startRow = it) }, KeyboardType.Number)
                    }
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_column_mapping), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    RuleFieldGroup {
                        RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_transaction_date), editing.dateColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_e_g_transaction_date), { draft = editing.copy(dateColumn = it) })
                        RuleDivider()
                        RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_amount), editing.amountColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_e_g_amount), { draft = editing.copy(amountColumn = it) })
                        RuleDivider()
                        RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_transaction_type), editing.typeColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_e_g_transaction_type), { draft = editing.copy(typeColumn = it) })
                        RuleDivider()
                        RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_category), editing.categoryColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_optional), { draft = editing.copy(categoryColumn = it) })
                        RuleDivider()
                        RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_note), editing.noteColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_optional), { draft = editing.copy(noteColumn = it) })
                    }
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_enter_column_names_from_your_file_date_and), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_defaults), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    RuleFieldGroup {
                        RuleChoiceDropdown(
                            label = textResources.getString(cn.turingzy.ledger.R.string.ui_default_account),
                            value = accounts.firstOrNull { it.account.id == editing.accountId }?.account?.name ?: textResources.getString(cn.turingzy.ledger.R.string.ui_choose_on_import),
                            options = listOf(RuleOption(null, textResources.getString(cn.turingzy.ledger.R.string.ui_choose_on_import))) + accounts.map { RuleOption(it.account.id, it.account.name) },
                            selectedId = editing.accountId,
                            onSelect = { draft = editing.copy(accountId = it.id) },
                            menuTestTag = "ruleAccountOptions",
                        )
                        RuleDivider()
                        RuleChoiceDropdown(
                            label = textResources.getString(cn.turingzy.ledger.R.string.ui_default_category),
                            value = categories.firstOrNull { it.category.id == editing.categoryId }?.category?.name ?: textResources.getString(cn.turingzy.ledger.R.string.ui_no_default_category),
                            options = listOf(RuleOption(null, textResources.getString(cn.turingzy.ledger.R.string.ui_no_default_category))) + categories.map { RuleOption(it.category.id, it.category.name) },
                            selectedId = editing.categoryId,
                            onSelect = { draft = editing.copy(categoryId = it.id) },
                            menuTestTag = "ruleCategoryOptions",
                        )
                    }
                    if (existing) {
                        BaseActionButton(variant = BaseButtonVariant.Text,
                            onClick = {
                                focusManager.clearFocus()
                                onDeleteRule(editing.id)
                                emptyDraft = ParseRulePreview(id = 0L)
                                draft = null
                            },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(vertical = LedgerStyle.spacing.medium),
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                        ) {
                            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_delete_rule), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepRuleEditorWindowStationary(enabled: Boolean) {
    val window = LocalActivity.current?.window
    if (!enabled || window == null) return
    LifecycleResumeEffect(window) {
        val previousMode = window.attributes.softInputMode
        // The form handles IME insets; letting Android pan the window also moves its background.
        val stationaryMode = (previousMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()) or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        window.setSoftInputMode(stationaryMode)
        onPauseOrDispose { window.setSoftInputMode(previousMode) }
    }
}

@Composable
private fun RuleFieldGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BaseCard(modifier, contentPadding = PaddingValues(LedgerStyle.spacing.none), contentSpacing = LedgerStyle.spacing.none, content = content)
}

@Composable
private fun RuleDivider() {
    BaseDivider(Modifier.padding(horizontal = LedgerStyle.spacing.large))
}

@Composable
private fun RuleTextField(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight).padding(horizontal = LedgerStyle.spacing.large), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(LedgerStyle.dimensions.formLabelWidth), style = MaterialTheme.typography.bodyLarge)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurfaceVariant, textAlign = TextAlign.End),
            cursorBrush = SolidColor(colors.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
            decorationBox = { field ->
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) Text(placeholder, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    field()
                }
            },
        )
    }
}

@Composable
private fun RuleChoiceDropdown(
    label: String,
    value: String,
    options: List<RuleOption>,
    selectedId: Long?,
    onSelect: (RuleOption) -> Unit,
    menuTestTag: String,
) {
    val colors = MaterialTheme.colorScheme
    LedgerDropdown(
        options = options,
        optionLabel = { it.label },
        isSelected = { it.id == selectedId },
        onSelect = onSelect,
        menuTestTag = menuTestTag,
    ) { anchorModifier, expanded, onToggle ->
        Box(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = label }
                .uiClickable(onClick = onToggle).padding(horizontal = LedgerStyle.spacing.large),
        ) {
            Row(
                anchorModifier.fillMaxWidth().heightIn(min = SettingsItemHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
            ) {
                Text(label, Modifier.width(LedgerStyle.dimensions.formLabelWidth), style = MaterialTheme.typography.bodyLarge)
                Text(value, Modifier.weight(1f), textAlign = TextAlign.End, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BaseDropdownIndicator(expanded)
            }
        }
    }
}
