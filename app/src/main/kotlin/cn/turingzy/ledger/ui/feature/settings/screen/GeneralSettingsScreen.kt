package cn.turingzy.ledger.ui.feature.settings.screen

import cn.turingzy.ledger.ui.feature.settings.component.SettingsItemHeight
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.component.base.BaseButtonVariant
import cn.turingzy.ledger.ui.component.base.BaseActionButton
import androidx.compose.foundation.layout.*
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.BaseIconButton
import cn.turingzy.ledger.ui.component.base.BaseCard
import cn.turingzy.ledger.ui.component.base.BaseDivider
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.AppSettingsViewModel
import cn.turingzy.ledger.ui.component.input.GeneralPreferencesForm
import cn.turingzy.ledger.ui.component.input.PreferenceChoice

@Composable
internal fun GeneralSettingsScreen(onBack: () -> Unit, viewModel: AppSettingsViewModel = hiltViewModel()) {
    val settings by viewModel.preferences.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val initial = settings ?: return
    var language by rememberSaveable(initial.language) { mutableStateOf(initial.language) }
    var theme by rememberSaveable(initial.theme) { mutableStateOf(initial.theme) }
    var primary by rememberSaveable(initial.primaryCurrency) { mutableStateOf(initial.primaryCurrency) }
    var from by rememberSaveable(primary) { mutableStateOf(if (primary == "USD") "CNY" else "USD") }
    var rate by rememberSaveable(from, primary) { mutableStateOf("") }
    var edited by rememberSaveable(from, primary) { mutableStateOf(false) }
    var rateDate by remember(from, primary) { mutableStateOf<String?>(null) }
    LaunchedEffect(from, primary, busy) {
        if (busy) return@LaunchedEffect
        try {
            val quote = viewModel.currentRate(from, primary)
            if (!edited) rate = quote.rate
            rateDate = quote.date
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) { timber.log.Timber.w(error, "No saved rate for settings preview") }
    }
    val draft = initial.copy(language = language, theme = theme, primaryCurrency = primary)
    val validRate = rate.toBigDecimalOrNull()?.let { it.signum() > 0 && it.scale() in -18..18 && it.precision() <= 36 } == true
    val canSave = !busy && (draft != initial || edited) && (!edited || validRate)
    GeneralSettingsContent(
        draft = draft, busy = busy, canSave = canSave,
        from = from, rate = rate, rateDate = rateDate, message = message,
        currencies = (accounts.map { it.account.currency } + listOf("USD", "CNY", "HKD", "EUR")).distinct().filter { it != primary }.map { it to it },
        onPreferencesChange = { language = it.language; theme = it.theme; primary = it.primaryCurrency },
        onFromChange = { from = it }, onRateChange = { rate = it; edited = true },
        onSave = { viewModel.save(draft, rateFrom = from.takeIf { edited }, rateValue = rate.takeIf { edited }, onSaved = { edited = false }) },
        onRefresh = viewModel::refreshRates, onBack = onBack,
    )
}

@Composable
internal fun GeneralSettingsContent(
    draft: AppPreferences,
    busy: Boolean,
    canSave: Boolean,
    from: String,
    rate: String,
    rateDate: String?,
    message: String?,
    currencies: List<Pair<String, String>>,
    onPreferencesChange: (AppPreferences) -> Unit,
    onFromChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    SettingsSubpageScaffold(
        title = stringResource(R.string.general_settings), onBack = onBack, modifier = modifier,
        actions = {
            BaseIconButton(MaterialSymbols.Check, stringResource(R.string.save), onSave,
                enabled = canSave, contentColor = colors.tertiary, showPressFeedback = false)
        },
    ) {
        Column(
            Modifier.fillMaxWidth().settingsPageScroll(),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
        ) {
            Text(stringResource(R.string.basic_preferences), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            GeneralPreferencesForm(draft, onPreferencesChange, !busy)
            Text(stringResource(R.string.exchange_rates), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            BaseCard(contentPadding = PaddingValues(LedgerStyle.spacing.none), contentSpacing = LedgerStyle.spacing.none) {
                PreferenceChoice(stringResource(R.string.account_currency), from, currencies, !busy, onFromChange)
                BaseDivider(Modifier.padding(horizontal = LedgerStyle.spacing.large))
                ExchangeRateField(from, draft.primaryCurrency, rate, !busy, onRateChange)
            }
            rateDate?.let {
                Text(stringResource(R.string.rate_as_of, it), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            BaseActionButton(variant = BaseButtonVariant.Text, onClick = onRefresh, enabled = !busy, contentPadding = PaddingValues(vertical = LedgerStyle.spacing.medium)) {
                Text(stringResource(R.string.refresh_rates))
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ExchangeRateField(from: String, primary: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val description = "1 $from, $primary"
    Row(Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight).padding(horizontal = LedgerStyle.spacing.large), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap)) {
        Text("1 $from", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
        BasicTextField(
            value = value, onValueChange = onChange, enabled = enabled, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled), textAlign = TextAlign.End),
            cursorBrush = SolidColor(colors.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).semantics { contentDescription = description },
            decorationBox = { field ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    if (value.isBlank()) Text("—", color = colors.onSurfaceVariant)
                    field()
                }
            },
        )
        Text(primary, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
}
