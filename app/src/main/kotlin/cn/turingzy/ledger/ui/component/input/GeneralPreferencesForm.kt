package cn.turingzy.ledger.ui.component.input

import cn.turingzy.ledger.ui.component.base.DropdownIndicatorBase
import androidx.compose.foundation.layout.*
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.DividerBase
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.model.settings.supportedCurrencyCodes

/**
 * 通用偏好设置表单卡片。
 *
 * 聚合语言（跟随系统/简体中文/英文）、主题模式（跟随系统/浅色/深色）以及本位币（主货币）的选择配置项。
 *
 * @param value 当前偏好设置配置 [AppPreferences]。
 * @param onChange 偏好设置变更时的回调。
 * @param enabled 是否允许用户交互，默认为 true。
 */
@Composable
internal fun GeneralPreferencesForm(value: AppPreferences, onChange: (AppPreferences) -> Unit, enabled: Boolean = true) {
    val currencies = remember(value.primaryCurrency) {
        (listOf(value.primaryCurrency, "CNY", "USD") + supportedCurrencyCodes.sorted()).distinct()
    }
    CardBase(
        contentPadding = PaddingValues(LedgerStyle.spacing.none),
        contentSpacing = LedgerStyle.spacing.none,
    ) {
        PreferenceChoice(stringResource(R.string.preference_language), value.language,
            listOf("system" to stringResource(R.string.follow_system), "zh" to "简体中文", "en" to "English"), enabled) { onChange(value.copy(language = it)) }
        DividerBase(Modifier.padding(horizontal = LedgerStyle.spacing.large))
        PreferenceChoice(stringResource(R.string.preference_theme), value.theme,
            listOf("system" to stringResource(R.string.follow_system), "light" to stringResource(R.string.light_theme), "dark" to stringResource(R.string.dark_theme)), enabled) { onChange(value.copy(theme = it)) }
        DividerBase(Modifier.padding(horizontal = LedgerStyle.spacing.large))
        PreferenceChoice(stringResource(R.string.primary_currency), value.primaryCurrency,
            currencies.map { it to it }, enabled) { onChange(value.copy(primaryCurrency = it)) }
    }
}

/**
 * 偏好设置单项选择行组件。
 *
 * 点击后以向下弹出的下拉菜单（[LedgerDropdown]）形式展示候选选项。
 *
 * @param title 设置项标题。
 * @param selected 当前选中项的 key。
 * @param options 待选选项列表，每个元素为 (key, 显示文本) 二元组。
 * @param enabled 是否允许交互。
 * @param onSelect 选中某个选项 key 时的回调。
 */
@Composable
internal fun PreferenceChoice(title: String, selected: String, options: List<Pair<String, String>>, enabled: Boolean = true, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    LedgerDropdown(
        options = options,
        optionLabel = { it.second },
        isSelected = { it.first == selected },
        onSelect = { if (enabled) onSelect(it.first) },
        direction = DropdownDirection.DOWN,
    ) { anchorModifier, expanded, opensUp, onToggle ->
        Box(
            Modifier.fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = title }
                .uiClickable(enabled = enabled, showPressFeedback = false, onClick = onToggle)
                .padding(horizontal = LedgerStyle.spacing.large),
        ) {
            Row(
                anchorModifier.fillMaxWidth().heightIn(min = LedgerStyle.dimensions.rowMinHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
            ) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled))
                Text(options.firstOrNull { it.first == selected }?.second ?: selected,
                    color = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled),
                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                DropdownIndicatorBase(expanded, opensUp = opensUp, contentColor = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled))
            }
        }
    }
}
