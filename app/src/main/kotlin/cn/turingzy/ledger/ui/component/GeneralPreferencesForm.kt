package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.AppPreferences
import cn.turingzy.ledger.model.supportedCurrencyCodes

@Composable
internal fun GeneralPreferencesForm(value: AppPreferences, onChange: (AppPreferences) -> Unit, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val currencies = remember(value.primaryCurrency) {
        (listOf(value.primaryCurrency, "CNY", "USD") + supportedCurrencyCodes.sorted()).distinct()
    }
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            PreferenceChoice(stringResource(R.string.preference_language), value.language,
                listOf("system" to stringResource(R.string.follow_system), "zh" to "简体中文", "en" to "English"), enabled) { onChange(value.copy(language = it)) }
            HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
            PreferenceChoice(stringResource(R.string.preference_theme), value.theme,
                listOf("system" to stringResource(R.string.follow_system), "light" to stringResource(R.string.light_theme), "dark" to stringResource(R.string.dark_theme)), enabled) { onChange(value.copy(theme = it)) }
            HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
            PreferenceChoice(stringResource(R.string.primary_currency), value.primaryCurrency,
                currencies.map { it to it }, enabled) { onChange(value.copy(primaryCurrency = it)) }
        }
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
        Row(
            anchorModifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) { contentDescription = title }
                .clickable(enabled = enabled, role = Role.Button, onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface.copy(alpha = if (enabled) 1f else 0.38f))
            Text(options.firstOrNull { it.first == selected }?.second ?: selected,
                color = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f),
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight, null, Modifier.size(24.dp),
                tint = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f))
        }
    }
}
