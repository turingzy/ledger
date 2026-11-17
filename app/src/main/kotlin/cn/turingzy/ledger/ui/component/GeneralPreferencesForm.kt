package cn.turingzy.ledger.ui.component

import cn.turingzy.ledger.ui.components.base.BaseDropdownIndicator
import androidx.compose.foundation.layout.*
import cn.turingzy.ledger.ui.components.base.BaseCard
import cn.turingzy.ledger.ui.components.base.BaseDivider
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
import cn.turingzy.ledger.model.AppPreferences
import cn.turingzy.ledger.model.supportedCurrencyCodes

@Composable
internal fun GeneralPreferencesForm(value: AppPreferences, onChange: (AppPreferences) -> Unit, enabled: Boolean = true) {
    val currencies = remember(value.primaryCurrency) {
        (listOf(value.primaryCurrency, "CNY", "USD") + supportedCurrencyCodes.sorted()).distinct()
    }
    BaseCard(
        contentPadding = PaddingValues(LedgerStyle.spacing.none),
        contentSpacing = LedgerStyle.spacing.none,
    ) {
        PreferenceChoice(stringResource(R.string.preference_language), value.language,
            listOf("system" to stringResource(R.string.follow_system), "zh" to "简体中文", "en" to "English"), enabled) { onChange(value.copy(language = it)) }
        BaseDivider(Modifier.padding(horizontal = LedgerStyle.spacing.large))
        PreferenceChoice(stringResource(R.string.preference_theme), value.theme,
            listOf("system" to stringResource(R.string.follow_system), "light" to stringResource(R.string.light_theme), "dark" to stringResource(R.string.dark_theme)), enabled) { onChange(value.copy(theme = it)) }
        BaseDivider(Modifier.padding(horizontal = LedgerStyle.spacing.large))
        PreferenceChoice(stringResource(R.string.primary_currency), value.primaryCurrency,
            currencies.map { it to it }, enabled) { onChange(value.copy(primaryCurrency = it)) }
    }
}

@Composable
internal fun PreferenceChoice(title: String, selected: String, options: List<Pair<String, String>>, enabled: Boolean = true, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    LedgerDropdown(
        options = options,
        optionLabel = { it.second },
        isSelected = { it.first == selected },
        onSelect = { if (enabled) onSelect(it.first) },
        direction = DropdownDirection.DOWN,
    ) { anchorModifier, expanded, onToggle ->
        Box(
            Modifier.fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = title }
                .uiClickable(enabled = enabled, onClick = onToggle)
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
                BaseDropdownIndicator(expanded, contentColor = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled))
            }
        }
    }
}
