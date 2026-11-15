package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cn.turingzy.ledger.ui.component.GeneralPreferencesForm
import cn.turingzy.ledger.ui.component.PreferenceChoice
import cn.turingzy.ledger.ui.component.LedgerIconButton

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
    val colors = MaterialTheme.colorScheme
    val draft = initial.copy(language = language, theme = theme, primaryCurrency = primary)
    val validRate = rate.toBigDecimalOrNull()?.let { it.signum() > 0 && it.scale() in -18..18 && it.precision() <= 36 } == true
    val canSave = !busy && (draft != initial || edited) && (!edited || validRate)
    SettingsPage(
        title = stringResource(R.string.general_settings),
        onDismissRequest = onBack,
        avoidKeyboard = true,
        action = {
            LedgerIconButton(
                onClick = {
                    viewModel.save(draft, rateFrom = from.takeIf { edited }, rateValue = rate.takeIf { edited }, onSaved = { edited = false })
                },
                enabled = canSave, showPressFeedback = false,
            ) {
                Icon(cn.turingzy.ledger.ui.component.MaterialSymbols.Check, stringResource(R.string.save),
                    tint = colors.tertiary.copy(alpha = if (canSave) 1f else 0.38f))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().settingsPageScroll(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.basic_preferences), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            GeneralPreferencesForm(draft, { language = it.language; theme = it.theme; primary = it.primaryCurrency }, !busy)
            Text(stringResource(R.string.exchange_rates), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    PreferenceChoice(stringResource(R.string.account_currency), from,
                        (accounts.map { it.account.currency } + listOf("USD", "CNY", "HKD", "EUR")).distinct().filter { it != primary }.map { it to it }, !busy) { from = it }
                    HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
                    ExchangeRateField(from, primary, rate, !busy) { rate = it; edited = true }
                }
            }
            rateDate?.let {
                Text(stringResource(R.string.rate_as_of, it), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            TextButton(onClick = viewModel::refreshRates, enabled = !busy, contentPadding = PaddingValues(vertical = 8.dp)) {
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
    Row(Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("1 $from", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
        BasicTextField(
            value = value, onValueChange = onChange, enabled = enabled, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f), textAlign = TextAlign.End),
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
