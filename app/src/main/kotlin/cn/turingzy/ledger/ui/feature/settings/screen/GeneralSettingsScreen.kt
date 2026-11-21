package cn.turingzy.ledger.ui.feature.settings.screen

import androidx.compose.animation.Crossfade
import cn.turingzy.ledger.ui.feature.settings.component.SettingsItemHeight
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.component.base.ButtonVariantBase
import cn.turingzy.ledger.ui.component.base.ActionButtonBase
import androidx.compose.foundation.layout.*
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.DividerBase
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.R
import cn.turingzy.ledger.data.repository.currency.ExchangeQuote
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import cn.turingzy.ledger.ui.AppStateViewModel
import cn.turingzy.ledger.ui.RateRefreshStatus
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.input.GeneralPreferencesForm
import cn.turingzy.ledger.ui.component.input.PreferenceChoice

@Composable
internal fun GeneralSettingsScreen(onBack: () -> Unit, viewModel: AppStateViewModel) {
    val settings by viewModel.preferences.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val refreshStatus by viewModel.rateRefreshStatus.collectAsStateWithLifecycle()
    val initial = settings ?: return
    val primary = initial.primaryCurrency
    var from by rememberSaveable(primary) { mutableStateOf(if (primary == "USD") "CNY" else "USD") }
    var rate by rememberSaveable(from, primary) { mutableStateOf("") }
    var edited by rememberSaveable(from, primary) { mutableStateOf(false) }
    var rateDate by remember(from, primary) { mutableStateOf<String?>(null) }
    var savedRateVersion by remember(from, primary) { mutableIntStateOf(0) }
    LaunchedEffect(from, primary, busy, savedRateVersion) {
        if (busy) return@LaunchedEffect
        try {
            val quote = viewModel.currentRate(from, primary)
            if (!edited) rate = quote.rate
            rateDate = quote.displayUpdateTime()
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) { timber.log.Timber.w(error, "No saved rate for settings preview") }
    }
    GeneralSettingsContent(
        draft = initial, busy = busy,
        from = from, rate = rate, rateDate = rateDate, message = message,
        currencies = (accounts.map { it.account.currency } + listOf("USD", "CNY", "HKD", "EUR")).distinct().filter { it != primary }.map { it to it },
        onPreferencesChange = { if (it != initial) viewModel.save(it) },
        onFromChange = { from = it },
        onRateChange = {
            rate = it
            edited = true
            viewModel.editManualRate(from, primary, it) {
                if (rate == it) {
                    edited = false
                    savedRateVersion++
                }
            }
        },
        onRefresh = { viewModel.refreshRates(from) }, onBack = onBack, refreshStatus = refreshStatus,
    )
}

@Composable
internal fun GeneralSettingsContent(
    draft: AppPreferences,
    busy: Boolean,
    from: String,
    rate: String,
    rateDate: String?,
    message: String?,
    currencies: List<Pair<String, String>>,
    onPreferencesChange: (AppPreferences) -> Unit,
    onFromChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    refreshStatus: RateRefreshStatus = RateRefreshStatus.IDLE,
) {
    val colors = MaterialTheme.colorScheme
    SettingsSubpageScaffold(
        title = stringResource(R.string.general_settings), onBack = onBack, modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().settingsPageScroll(),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
        ) {
            Text(stringResource(R.string.basic_preferences), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            GeneralPreferencesForm(draft, onPreferencesChange, !busy)
            Text(stringResource(R.string.exchange_rates), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            CardBase(contentPadding = PaddingValues(LedgerStyle.spacing.none), contentSpacing = LedgerStyle.spacing.none) {
                PreferenceChoice(stringResource(R.string.account_currency), from, currencies, !busy, onFromChange)
                DividerBase(Modifier.padding(horizontal = LedgerStyle.spacing.large))
                ExchangeRateField(from, draft.primaryCurrency, rate, !busy, onRateChange)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                ) {
                    Text(rateDate?.let { stringResource(R.string.rate_as_of, it) } ?: stringResource(R.string.reference_rate),
                        Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Crossfade(refreshStatus, label = "RateRefreshStatus") { status ->
                        when (status) {
                            RateRefreshStatus.IDLE -> Unit
                            RateRefreshStatus.REFRESHING -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            RateRefreshStatus.SUCCESS -> Icon(MaterialSymbols.Check, stringResource(R.string.rates_refreshed),
                                Modifier.size(16.dp), tint = colors.tertiary)
                        }
                    }
                }
                ActionButtonBase(variant = ButtonVariantBase.Text, onClick = onRefresh, enabled = !busy,
                    showPressFeedback = false,
                    contentPadding = PaddingValues(vertical = LedgerStyle.spacing.medium)) {
                    Text(stringResource(R.string.refresh_rates), style = MaterialTheme.typography.bodyMedium)
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
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

/** Shows actual local update time when known, otherwise the provider's date. */
private fun ExchangeQuote.displayUpdateTime(): String = updatedAtMillis?.let {
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
} ?: DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDate.parse(date))
