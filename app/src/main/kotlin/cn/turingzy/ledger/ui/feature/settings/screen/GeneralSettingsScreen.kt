package cn.turingzy.ledger.ui.feature.settings.screen

import androidx.compose.animation.Crossfade
import cn.turingzy.ledger.ui.feature.settings.component.SettingsItemHeight
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.component.base.ButtonVariantBase
import cn.turingzy.ledger.ui.component.base.ActionButtonBase
import androidx.compose.foundation.layout.*
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.model.currency.convertCents
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.ListRowDividerBase
import cn.turingzy.ledger.ui.component.base.SectionHeaderBase
import cn.turingzy.ledger.ui.component.base.SectionHeaderVariantBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.input.FormInputBase
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
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
import cn.turingzy.ledger.ui.component.biz.currencyOptionsBiz

/**
 * 通用设置界面屏幕。
 *
 * 负责绑定全局 [AppStateViewModel]，提供应用通用偏好配置（如本位币选择等）
 * 以及多币种汇率管理（汇率查看、手动录入与远程拉取最新汇率）等交互。
 *
 * @param onBack 返回上一级页面的回调
 * @param viewModel 全局应用状态 ViewModel
 */
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
        currencies = (accounts.map { it.account.currency } + listOf("USD", "CNY", "HKD", "EUR")).distinct().filter { it != primary },
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

/**
 * 通用设置的内容布局组件。
 *
 * 包含基础偏好设置表单区域和多币种汇率查询/维护区域。
 *
 * @param draft 当前的偏好设置草稿
 * @param busy 是否处于全局忙碌状态
 * @param from 正在查看汇率的源货币代码
 * @param rate 汇率转换比例字符串
 * @param rateDate 汇率生效或更新日期文字
 * @param message 界面底部错误或反馈提示
 * @param currencies 可选的汇率换算源货币列表
 * @param onPreferencesChange 偏好设置变更回调
 * @param onFromChange 选中的源货币变更回调
 * @param onRateChange 汇率数值修改回调
 * @param onRefresh 点击在线刷新汇率回调
 * @param onBack 返回回调
 * @param modifier 修饰符
 * @param refreshStatus 当前汇率在线刷新的状态
 */
@Composable
internal fun GeneralSettingsContent(
    draft: AppPreferences,
    busy: Boolean,
    from: String,
    rate: String,
    rateDate: String?,
    message: String?,
    currencies: List<String>,
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
            GeneralSettingsSection(stringResource(R.string.basic_preferences)) {
                GeneralPreferencesForm(draft, onPreferencesChange, !busy)
            }
            GeneralSettingsSection(stringResource(R.string.exchange_rates)) {
                CardBase(contentPadding = PaddingValues(LedgerStyle.spacing.none), contentSpacing = LedgerStyle.spacing.none) {
                    PreferenceChoice(
                        title = stringResource(R.string.account_currency),
                        selected = from,
                        options = currencyOptionsBiz(currencies),
                        enabled = !busy,
                        selectedLabel = from,
                        menuWidth = LedgerStyle.dimensions.currencyDropdownWidth,
                        horizontalPadding = LedgerStyle.spacing.rowHorizontal,
                        onSelect = onFromChange,
                    )
                    ListRowDividerBase()
                    ExchangeRateField(from, draft.primaryCurrency, rate, !busy, onRateChange)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                ) {
                    Text(rateDate?.let { stringResource(R.string.rate_as_of, it) } ?: stringResource(R.string.reference_rate),
                        Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, color = LedgerStyle.colors.textSupporting)
                    Crossfade(refreshStatus, label = "RateRefreshStatus") { status ->
                        when (status) {
                            RateRefreshStatus.IDLE -> Unit
                            RateRefreshStatus.REFRESHING -> CircularProgressIndicator(Modifier.size(LedgerStyle.icons.small), strokeWidth = 2.dp)
                            RateRefreshStatus.SUCCESS -> Icon(MaterialSymbols.Check, stringResource(R.string.rates_refreshed),
                                Modifier.size(LedgerStyle.icons.small), tint = LedgerStyle.colors.success)
                        }
                    }
                }
                ActionButtonBase(variant = ButtonVariantBase.Text, onClick = onRefresh, enabled = !busy,
                    showPressFeedback = false,
                    contentPadding = PaddingValues(vertical = LedgerStyle.spacing.medium)) {
                    Text(stringResource(R.string.refresh_rates), style = MaterialTheme.typography.bodyMedium)
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
        }
    }
}

/** 分组标题与卡片之间的留白小于分组之间的留白。 */
@Composable
private fun GeneralSettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
        SectionHeaderBase(title, variant = SectionHeaderVariantBase.Supporting)
        content()
    }
}

/**
 * 汇率输入字段组件。
 *
 * 呈现 `1 [from] = [输入框] [primary]` 的换算布局，支持输入十进制数值。
 *
 * @param from 源货币代码
 * @param primary 主本位币代码
 * @param value 当前汇率文本
 * @param enabled 是否允许输入编辑
 * @param onChange 汇率数值修改回调
 */
@Composable
private fun ExchangeRateField(from: String, primary: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val description = "1 $from, $primary"
    val rateError = if (value.isNotBlank() && runCatching { convertCents(0L, value) }.isFailure) {
        stringResource(R.string.ui_enter_a_valid_exchange_rate)
    } else null
    val opacity = if (enabled) 1f else LedgerStyle.opacity.disabled
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight).padding(horizontal = LedgerStyle.spacing.rowHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
        ) {
            Text("1 $from", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface.copy(alpha = opacity))
            FormInputBase(
                value = value, onValueChange = onChange, enabled = enabled, isError = rateError != null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurfaceVariant, textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).semantics {
                    contentDescription = description
                    if (enabled && rateError != null) error(rateError)
                },
                decorationBox = { field ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        if (value.isBlank()) Text("—", color = LedgerStyle.colors.textSupporting.copy(alpha = opacity))
                        field()
                    }
                },
            )
            Text(primary, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant.copy(alpha = opacity))
        }
        if (enabled && rateError != null) {
            Text(
                rateError,
                Modifier.padding(horizontal = LedgerStyle.spacing.rowHorizontal).padding(bottom = LedgerStyle.spacing.medium),
                style = MaterialTheme.typography.bodySmall, color = colors.error,
            )
        }
    }
}

/**
 * 格式化并展示汇率的更新时间。
 *
 * 若记录了本地更新时间戳则转换为本地日期时间字符串，否则展示提供商的日期。
 */
private fun ExchangeQuote.displayUpdateTime(): String = updatedAtMillis?.let {
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
} ?: DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDate.parse(date))
