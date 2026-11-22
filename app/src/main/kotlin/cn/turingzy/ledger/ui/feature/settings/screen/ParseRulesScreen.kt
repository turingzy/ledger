package cn.turingzy.ledger.ui.feature.settings.screen

import cn.turingzy.ledger.R
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.ui.feature.settings.component.SettingsActionRow
import cn.turingzy.ledger.ui.feature.settings.component.SettingsItemHeight
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageContentPadding
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.component.base.DropdownIndicatorBase
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cn.turingzy.ledger.navigation.SettingsEditorList
import cn.turingzy.ledger.navigation.SettingsEditorNavHost
import cn.turingzy.ledger.navigation.openSettingsCreatePage
import cn.turingzy.ledger.navigation.settingsCreatePage
import kotlinx.serialization.Serializable
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.input.LedgerDropdown
import androidx.compose.foundation.layout.Box
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.DividerBase
import cn.turingzy.ledger.ui.component.base.IconButtonBase
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.feature.settings.component.SettingsDeleteAction
import cn.turingzy.ledger.ui.theme.LedgerStyle
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon

internal typealias ParseRulePreview = cn.turingzy.ledger.model.transfer.BillParseRule

internal fun defaultParseRulePreviews(): List<ParseRulePreview> = ParseRulePreview.defaults()

@Serializable
private data class RuleDetail(val id: Long)

private data class RuleOption(val id: Long?, val label: String)

@Composable
internal fun ParseRulesScreen(
    accounts: List<AccountWithTemplate>,
    categories: List<CategoryWithIcon>,
    rules: List<ParseRulePreview>,
    onSaveRule: (ParseRulePreview) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val controller = rememberNavController()
    val currentRules by rememberUpdatedState(rules)
    val currentAccounts by rememberUpdatedState(accounts)
    val currentCategories by rememberUpdatedState(categories)
    val saveRule by rememberUpdatedState(onSaveRule)
    val deleteRule by rememberUpdatedState(onDeleteRule)
    val dismiss by rememberUpdatedState(onDismissRequest)
    SettingsEditorNavHost(controller) {
        composable(SettingsEditorList) {
            ParseRuleList(
                rules = currentRules,
                onBack = dismiss,
                onCreate = controller::openSettingsCreatePage,
                onOpenRule = { controller.navigate(RuleDetail(it)) { launchSingleTop = true } },
            )
        }
        composable<RuleDetail> { entry ->
            val id = entry.toRoute<RuleDetail>().id
            val rule = currentRules.firstOrNull { it.id == id }
            var initialRule by remember(entry) { mutableStateOf(rule) }
            LaunchedEffect(rule) {
                if (initialRule == null) initialRule = rule
            }
            val original = initialRule ?: rule
            if (original != null) ParseRuleEditor(
                original, currentAccounts, currentCategories, saveRule,
                onClose = { controller.popBackStack() },
                onDelete = {
                    deleteRule(id)
                    controller.popBackStack()
                },
            )
        }
        settingsCreatePage {
            val original = remember {
                ParseRulePreview(id = (currentRules.maxOfOrNull { it.id } ?: 0L) + 1L)
            }
            ParseRuleEditor(original, currentAccounts, currentCategories, saveRule, onClose = { controller.popBackStack() })
        }
    }
}

/** Opens existing rules as details and keeps creation behind the Add action. */
@Composable
private fun ParseRuleList(
    rules: List<ParseRulePreview>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenRule: (Long) -> Unit,
) {
    SettingsSubpageScaffold(
        title = stringResource(R.string.ui_import_rules),
        onBack = onBack,
        actions = { IconButtonBase(MaterialSymbols.Add, stringResource(R.string.ui_new_import_rule), onCreate) },
    ) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = settingsPageContentPadding(),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
        ) {
            if (rules.isEmpty()) item {
                Text(stringResource(R.string.ui_no_import_rules), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(rules, key = { it.id }) { rule ->
                SettingsActionRow(
                    title = rule.name,
                    subtitle = stringResource(R.string.rule_start_row_summary, rule.startRow),
                    onClick = { onOpenRule(rule.id) },
                )
            }
        }
    }
}

/** Saves valid detail edits immediately and commits a new rule only on confirmation. */
@Composable
internal fun ParseRuleEditor(
    initialDraft: ParseRulePreview,
    accounts: List<AccountWithTemplate>,
    categories: List<CategoryWithIcon>,
    onSaveRule: (ParseRulePreview) -> Unit,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var draft by remember { mutableStateOf(initialDraft) }
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val existing = onDelete != null
    KeepRuleEditorWindowStationary()

    fun updateRule(value: ParseRulePreview) {
        if (value == draft) return
        draft = value
        if (existing && value.valid) onSaveRule(value.copy(name = value.name.trim()))
    }

    SettingsSubpageScaffold(
        title = stringResource(if (existing) R.string.ui_rule_details else R.string.ui_new_rule),
        modifier = Modifier.testTag("parseRulePage"),
        onBack = {
            focusManager.clearFocus()
            onClose()
        },
        closeInsteadOfBack = !existing,
        actions = {
            if (!existing) {
                IconButtonBase(
                    imageVector = MaterialSymbols.Check,
                    contentDescription = stringResource(R.string.ui_save_rule),
                    enabled = draft.valid,
                    contentColor = colors.tertiary,
                    onClick = {
                        focusManager.clearFocus()
                        onSaveRule(draft.copy(name = draft.name.trim()))
                        onClose()
                    },
                )
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().settingsPageScroll().testTag("parseRuleForm"),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
        ) {
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_basic_information), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            RuleFieldGroup {
                RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_rule_name), draft.name, textResources.getString(cn.turingzy.ledger.R.string.ui_enter_a_name), { updateRule(draft.copy(name = it)) })
                RuleDivider()
                RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_starting_row), draft.startRow, textResources.getString(cn.turingzy.ledger.R.string.ui_first_data_row), { updateRule(draft.copy(startRow = it)) }, KeyboardType.Number)
            }
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_column_mapping), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            RuleFieldGroup {
                RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_transaction_date), draft.dateColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_e_g_transaction_date), { updateRule(draft.copy(dateColumn = it)) })
                RuleDivider()
                RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_amount), draft.amountColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_e_g_amount), { updateRule(draft.copy(amountColumn = it)) })
                RuleDivider()
                RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_transaction_type), draft.typeColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_e_g_transaction_type), { updateRule(draft.copy(typeColumn = it)) })
                RuleDivider()
                RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_category), draft.categoryColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_optional), { updateRule(draft.copy(categoryColumn = it)) })
                RuleDivider()
                RuleTextField(textResources.getString(cn.turingzy.ledger.R.string.ui_note), draft.noteColumn, textResources.getString(cn.turingzy.ledger.R.string.ui_optional), { updateRule(draft.copy(noteColumn = it)) })
            }
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_enter_column_names_from_your_file_date_and), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_defaults), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            RuleFieldGroup {
                RuleChoiceDropdown(
                    label = textResources.getString(cn.turingzy.ledger.R.string.ui_default_account),
                    value = accounts.firstOrNull { it.account.id == draft.accountId }?.account?.name ?: textResources.getString(cn.turingzy.ledger.R.string.ui_choose_on_import),
                    options = listOf(RuleOption(null, textResources.getString(cn.turingzy.ledger.R.string.ui_choose_on_import))) + accounts.map { RuleOption(it.account.id, it.account.name) },
                    selectedId = draft.accountId,
                    onSelect = { updateRule(draft.copy(accountId = it.id)) },
                    menuTestTag = "ruleAccountOptions",
                )
                RuleDivider()
                RuleChoiceDropdown(
                    label = textResources.getString(cn.turingzy.ledger.R.string.ui_default_category),
                    value = categories.firstOrNull { it.category.id == draft.categoryId }?.category?.name ?: textResources.getString(cn.turingzy.ledger.R.string.ui_no_default_category),
                    options = listOf(RuleOption(null, textResources.getString(cn.turingzy.ledger.R.string.ui_no_default_category))) + categories.map { RuleOption(it.category.id, it.category.name) },
                    selectedId = draft.categoryId,
                    onSelect = { updateRule(draft.copy(categoryId = it.id)) },
                    menuTestTag = "ruleCategoryOptions",
                )
            }
            if (onDelete != null) {
                SettingsDeleteAction(
                    itemId = draft.id,
                    title = stringResource(R.string.ui_delete_rule),
                    onDelete = {
                        focusManager.clearFocus()
                        onDelete()
                    },
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun KeepRuleEditorWindowStationary() {
    val window = LocalActivity.current?.window ?: return
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
    CardBase(modifier, contentPadding = PaddingValues(LedgerStyle.spacing.none), contentSpacing = LedgerStyle.spacing.none, content = content)
}

@Composable
private fun RuleDivider() {
    DividerBase(Modifier.padding(horizontal = LedgerStyle.spacing.large))
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
    ) { anchorModifier, expanded, opensUp, onToggle ->
        Box(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = label }
                .uiClickable(showPressFeedback = false, onClick = onToggle).padding(horizontal = LedgerStyle.spacing.large),
        ) {
            Row(
                anchorModifier.fillMaxWidth().heightIn(min = SettingsItemHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
            ) {
                Text(label, Modifier.width(LedgerStyle.dimensions.formLabelWidth), style = MaterialTheme.typography.bodyLarge)
                Text(value, Modifier.weight(1f), textAlign = TextAlign.End, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                DropdownIndicatorBase(expanded, opensUp = opensUp)
            }
        }
    }
}
