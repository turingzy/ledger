package cn.turingzy.ledger.ui.feature.settings

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cn.turingzy.ledger.navigation.SettingsSubpageTransition
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.LedgerDropdown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import cn.turingzy.ledger.ui.component.LedgerIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryWithIcon

internal typealias ParseRulePreview = cn.turingzy.ledger.model.BillParseRule

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
) {
    var draft by remember { mutableStateOf<ParseRulePreview?>(null) }
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
            editing == null -> "账单解析规则"
            existing -> "规则详情"
            else -> "新增规则"
        }

        SettingsPage(
            title = title,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.testTag("parseRulePage"),
            avoidKeyboard = editing != null,
            onBack = if (editing != null) ({
                focusManager.clearFocus()
                if (rules.isEmpty()) onDismissRequest() else draft = null
            }) else null,
            action = {
                IconButton(
                    showPressFeedback = false,
                    enabled = editing == null || editing.valid,
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
                ) {
                    Icon(
                        if (editing == null) MaterialSymbols.Add else MaterialSymbols.Check,
                        if (editing == null) "新增解析规则" else "保存规则",
                        tint = if (editing == null) colors.onSurfaceVariant else colors.tertiary.copy(alpha = if (editing.valid) 1f else 0.38f),
                    )
                }
            },
        ) {
            if (editing == null) {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = SettingsPageBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("基本信息", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    RuleFieldGroup {
                        RuleTextField("规则名称", editing.name, "请输入名称", { draft = editing.copy(name = it) })
                        RuleDivider()
                        RuleTextField("起始行", editing.startRow, "从第几行读取", { draft = editing.copy(startRow = it) }, KeyboardType.Number)
                    }
                    Text("字段对应", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    RuleFieldGroup {
                        RuleTextField("交易时间", editing.dateColumn, "如：交易时间", { draft = editing.copy(dateColumn = it) })
                        RuleDivider()
                        RuleTextField("金额", editing.amountColumn, "如：金额", { draft = editing.copy(amountColumn = it) })
                        RuleDivider()
                        RuleTextField("收支类型", editing.typeColumn, "如：收支类型", { draft = editing.copy(typeColumn = it) })
                        RuleDivider()
                        RuleTextField("分类", editing.categoryColumn, "选填", { draft = editing.copy(categoryColumn = it) })
                        RuleDivider()
                        RuleTextField("备注", editing.noteColumn, "选填", { draft = editing.copy(noteColumn = it) })
                    }
                    Text("填写原始账单列名；交易时间和金额必填。未设置收支列时按支出导入。", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Text("默认归属", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    RuleFieldGroup {
                        RuleChoiceDropdown(
                            label = "默认账户",
                            value = accounts.firstOrNull { it.account.id == editing.accountId }?.account?.name ?: "导入时选择",
                            options = listOf(RuleOption(null, "导入时选择")) + accounts.map { RuleOption(it.account.id, it.account.name) },
                            selectedId = editing.accountId,
                            onSelect = { draft = editing.copy(accountId = it.id) },
                            menuTestTag = "ruleAccountOptions",
                        )
                        RuleDivider()
                        RuleChoiceDropdown(
                            label = "默认分类",
                            value = categories.firstOrNull { it.category.id == editing.categoryId }?.category?.name ?: "不指定分类",
                            options = listOf(RuleOption(null, "不指定分类")) + categories.map { RuleOption(it.category.id, it.category.name) },
                            selectedId = editing.categoryId,
                            onSelect = { draft = editing.copy(categoryId = it.id) },
                            menuTestTag = "ruleCategoryOptions",
                        )
                    }
                    if (existing) {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                onDeleteRule(editing.id)
                                emptyDraft = ParseRulePreview(id = 0L)
                                draft = null
                            },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                        ) {
                            Text("删除规则", style = MaterialTheme.typography.bodyLarge)
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
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), content = content)
    }
}

@Composable
private fun RuleDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
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
    Row(Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
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
    ) { anchorModifier, _, onToggle ->
        Row(
            anchorModifier.fillMaxWidth().heightIn(min = SettingsItemHeight)
                .semantics(mergeDescendants = true) { contentDescription = label }
                .clickable(role = Role.Button, onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
            Text(value, Modifier.weight(1f), textAlign = TextAlign.End, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(MaterialSymbols.ChevronRight, null, Modifier.size(24.dp), tint = colors.onSurfaceVariant)
        }
    }
}
