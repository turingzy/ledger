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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.model.settings.supportedCurrencyCodes
import cn.turingzy.ledger.ui.component.biz.currencyOptionsBiz

/**
 * 通用偏好设置表单卡片。
 *
 * 提供简体中文、英语、主题模式和展示货币的选择。
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
    val locale = LocalConfiguration.current.locales[0]
    val language = when (value.language) {
        "zh", "en" -> value.language
        else -> if (locale.language == "en") "en" else "zh"
    }
    CardBase(
        contentPadding = PaddingValues(LedgerStyle.spacing.none),
        contentSpacing = LedgerStyle.spacing.none,
    ) {
        PreferenceChoice(stringResource(R.string.preference_language), language,
            listOf("zh" to "简体中文", "en" to "English"), enabled) { onChange(value.copy(language = it)) }
        DividerBase(Modifier.padding(horizontal = LedgerStyle.spacing.large))
        PreferenceChoice(stringResource(R.string.preference_theme), value.theme,
            listOf("system" to stringResource(R.string.follow_system), "light" to stringResource(R.string.light_theme), "dark" to stringResource(R.string.dark_theme)), enabled) { onChange(value.copy(theme = it)) }
        DividerBase(Modifier.padding(horizontal = LedgerStyle.spacing.large))
        PreferenceChoice(
            title = stringResource(R.string.display_currency),
            selected = value.primaryCurrency,
            options = currencyOptionsBiz(currencies),
            enabled = enabled,
            selectedLabel = value.primaryCurrency,
            menuWidth = LedgerStyle.dimensions.currencyDropdownWidth,
            onSelect = { onChange(value.copy(primaryCurrency = it)) },
        )
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
 * @param selectedLabel 收起时的显示值，默认沿用菜单里的选项文案。
 * @param menuWidth 弹出菜单的宽度。
 * @param onSelect 选中某个选项 key 时的回调。
 */
@Composable
internal fun PreferenceChoice(
    title: String,
    selected: String,
    options: List<Pair<String, String>>,
    enabled: Boolean = true,
    selectedLabel: String = options.firstOrNull { it.first == selected }?.second ?: selected,
    menuWidth: Dp = LedgerStyle.dimensions.dropdownWidth,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    LedgerDropdown(
        options = options,
        optionLabel = { it.second },
        isSelected = { it.first == selected },
        onSelect = { if (enabled) onSelect(it.first) },
        direction = DropdownDirection.DOWN,
        menuWidth = menuWidth,
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
                Text(selectedLabel,
                    color = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled),
                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                DropdownIndicatorBase(expanded, opensUp = opensUp, contentColor = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled))
            }
        }
    }
}
