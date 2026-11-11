package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.LocalIndication
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import cn.turingzy.ledger.ui.component.LedgerIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryWithIcon

internal data class ParseRulePreview(
    val id: Long,
    val name: String = "",
    val startRow: String = "2",
    val dateColumn: String = "",
    val amountColumn: String = "",
    val typeColumn: String = "",
    val categoryColumn: String = "",
    val noteColumn: String = "",
    val accountId: Long? = null,
    val categoryId: Long? = null,
) {
    val valid: Boolean get() = name.isNotBlank() && (startRow.toIntOrNull() ?: 0) > 0 && dateColumn.isNotBlank() && amountColumn.isNotBlank()
}

private enum class RuleEditorSection(val label: String) { BASIC("基本信息"), FIELDS("字段对应"), DEFAULTS("默认归属") }

private enum class RuleChoice { ACCOUNT, CATEGORY }
private data class RuleOption(val id: Long?, val label: String)

@Composable
internal fun ParseRulesSheet(
    accounts: List<AccountWithTemplate>,
    categories: List<CategoryWithIcon>,
    rules: List<ParseRulePreview>,
    onSaveRule: (ParseRulePreview) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var draft by remember { mutableStateOf<ParseRulePreview?>(null) }
    var section by remember { mutableStateOf(RuleEditorSection.BASIC) }
    var choice by remember { mutableStateOf<RuleChoice?>(null) }
    val editing = draft
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val existing = editing != null && rules.any { it.id == editing.id }
    val title = when {
        choice == RuleChoice.ACCOUNT -> "默认账户"
        choice == RuleChoice.CATEGORY -> "默认分类"
        editing == null -> "账单解析规则"
        existing -> "规则详情"
        else -> "新增规则"
    }

    SettingsBottomSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        onBack = if (editing != null) ({
            focusManager.clearFocus()
            if (choice != null) choice = null else draft = null
        }) else null,
        action = {
            if (choice == null) {
                IconButton(
                    showPressFeedback = false,
                    enabled = editing == null || editing.valid,
                    onClick = {
                        focusManager.clearFocus()
                        if (editing == null) {
                            section = RuleEditorSection.BASIC
                            draft = ParseRulePreview(id = (rules.maxOfOrNull { it.id } ?: 0L) + 1L)
                        } else {
                            onSaveRule(editing.copy(name = editing.name.trim()))
                            draft = null
                        }
                    },
                ) {
                    Icon(
                        if (editing == null) MaterialSymbols.Add else MaterialSymbols.Check,
                        if (editing == null) "新增解析规则" else "保存规则预览",
                        tint = if (editing == null) colors.onSurfaceVariant else colors.tertiary.copy(alpha = if (editing.valid) 1f else 0.38f),
                    )
                }
            }
        },
    ) {
        if (editing == null) {
            if (rules.isEmpty()) {
                Text(
                    "暂无解析规则，点击右上角添加",
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                )
            } else {
                RuleFieldGroup {
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(rules, key = { it.id }) { rule ->
                            Row(
                                Modifier.fillMaxWidth().clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = LocalIndication.current,
                                ) { section = RuleEditorSection.BASIC; draft = rule }.padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(rule.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("从第 ${rule.startRow} 行读取", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                                Icon(MaterialSymbols.ChevronRight, "编辑规则", tint = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Text("当前为交互预览，修改不会保存到设备。", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        } else if (choice != null) {
            val options = if (choice == RuleChoice.ACCOUNT) {
                listOf(RuleOption(null, "导入时选择")) + accounts.map { RuleOption(it.account.id, it.account.name) }
            } else {
                listOf(RuleOption(null, "不指定分类")) + categories.map { RuleOption(it.category.id, it.category.name) }
            }
            val selectedId = if (choice == RuleChoice.ACCOUNT) editing.accountId else editing.categoryId
            RuleFieldGroup {
                LazyColumn(Modifier.heightIn(max = 280.dp)) {
                    items(options, key = { it.id ?: "none" }) { option ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                draft = if (choice == RuleChoice.ACCOUNT) editing.copy(accountId = option.id)
                                else editing.copy(categoryId = option.id)
                                choice = null
                            }.padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(option.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            if (selectedId == option.id) Icon(MaterialSymbols.Check, "已选择", tint = colors.tertiary)
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LedgerSegmentedControl(
                    labels = RuleEditorSection.entries.map { it.label },
                    selectedIndex = RuleEditorSection.entries.indexOf(section),
                    onSelect = { focusManager.clearFocus(); section = RuleEditorSection.entries[it] },
                    modifier = Modifier.fillMaxWidth(),
                )
                key(section) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (section) {
                            RuleEditorSection.BASIC -> {
                                RuleFieldGroup {
                                    RuleTextField("规则名称", editing.name, "请输入名称", { draft = editing.copy(name = it) })
                                    RuleDivider()
                                    RuleTextField("起始行", editing.startRow, "从第几行读取", { draft = editing.copy(startRow = it) }, KeyboardType.Number)
                                }
                                Text("名称和起始行为必填项。", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            RuleEditorSection.FIELDS -> {
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
                                Text("填写原始账单列名；交易时间和金额必填。", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            RuleEditorSection.DEFAULTS -> {
                                RuleFieldGroup {
                                    RuleChoiceRow("默认账户", accounts.firstOrNull { it.account.id == editing.accountId }?.account?.name ?: "导入时选择") {
                                        focusManager.clearFocus()
                                        choice = RuleChoice.ACCOUNT
                                    }
                                    RuleDivider()
                                    RuleChoiceRow("默认分类", categories.firstOrNull { it.category.id == editing.categoryId }?.category?.name ?: "不指定分类") {
                                        focusManager.clearFocus()
                                        choice = RuleChoice.CATEGORY
                                    }
                                    if (existing) {
                                        RuleDivider()
                                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("删除规则", Modifier.weight(1f))
                                            IconButton(showPressFeedback = false, onClick = { onDeleteRule(editing.id); draft = null }) {
                                                Icon(MaterialSymbols.Delete, "删除规则预览", tint = colors.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleFieldGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), content = content)
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
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, textAlign = TextAlign.End),
            cursorBrush = SolidColor(colors.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f),
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
private fun RuleChoiceRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
        Text(value, Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(MaterialSymbols.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
