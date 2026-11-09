package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.util.AmountFormatter

private enum class AccountTab(val title: String) {
    ASSET("储蓄卡 / 资产"),
    DEBT("信用卡 / 负债"),
}

private data class ThemeOption(
    val name: String,
    val dotColor: Color,
    val templateId: Long?,
)

private val DefaultThemeOptions = listOf(
    ThemeOption("黑曜钛金", Color(0xFF1C1C1E), 2L),
    ThemeOption("经典深蓝", Color(0xFF007AFF), 1L),
    ThemeOption("活力薄荷", Color(0xFF34C759), 3L),
    ThemeOption("极光深紫", Color(0xFF5856D6), 2L),
    ThemeOption("暖阳金橙", Color(0xFFFF9500), 1L),
)

private val DefaultInstitutions = listOf(
    "招商银行",
    "中国工商银行",
    "中国建设银行",
    "中国农业银行",
    "中国银行",
    "交通银行",
    "中信银行",
    "浦发银行",
    "民生银行",
    "兴业银行",
    "平安银行",
    "广发银行",
    "微信支付",
    "支付宝",
    "现金",
)

private val DefaultCurrencies = listOf(
    "CNY" to "CNY 人民币",
    "USD" to "USD 美元",
    "HKD" to "HKD 港币",
    "EUR" to "EUR 欧元",
    "JPY" to "JPY 日元",
    "GBP" to "GBP 英镑",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean,
        currency: String,
        templateId: Long?,
    ) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
        modifier = modifier,
    ) {
        AddAccountSheetContent(
            onDismissRequest = onDismissRequest,
            onConfirm = onConfirm,
        )
    }
}

@Composable
fun AddAccountSheetContent(
    onDismissRequest: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean,
        currency: String,
        templateId: Long?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(AccountTab.ASSET) }
    var selectedInstitution by rememberSaveable { mutableStateOf("招商银行") }
    var name by rememberSaveable { mutableStateOf("招商银行储蓄卡") }
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var balanceText by rememberSaveable { mutableStateOf("") }
    var includeInTotal by rememberSaveable { mutableStateOf(true) }
    var selectedCurrency by rememberSaveable { mutableStateOf("CNY") }
    var selectedCurrencyDisplay by rememberSaveable { mutableStateOf("CNY 人民币") }
    var selectedThemeName by rememberSaveable { mutableStateOf(DefaultThemeOptions.first().name) }
    val selectedTheme = DefaultThemeOptions.first { it.name == selectedThemeName }

    var isInstitutionMenuExpanded by remember { mutableStateOf(false) }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }
    var isThemeMenuExpanded by remember { mutableStateOf(false) }

    var isNoteExpanded by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val parsedBalance = if (balanceText.isBlank()) 0L else AmountFormatter.parseToCents(balanceText)
    val isBalanceValid = parsedBalance != null
    val canConfirm = name.isNotBlank() && isBalanceValid

    val currentAccountType = when (selectedTab) {
        AccountTab.DEBT -> AccountType.CREDIT
        AccountTab.ASSET -> when (selectedInstitution) {
            "微信支付", "支付宝" -> AccountType.WALLET
            "现金" -> AccountType.CASH
            else -> AccountType.DEBIT
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            // 顶栏：圆形关闭按钮 + “新建”大标题 + 蓝色圆形确认完成按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左侧圆形关闭按钮
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 16.dp),
                            onClick = onDismissRequest,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }

                // 居中标题
                Text(
                    text = "新建",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // 右侧蓝色圆形完成按钮
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (canConfirm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        .clickable(
                            enabled = canConfirm,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 16.dp),
                            onClick = {
                                val cents = parsedBalance ?: return@clickable
                                focusManager.clearFocus()
                                onConfirm(
                                    name.trim(),
                                    currentAccountType,
                                    cents,
                                    cardNumber.trim().takeIf { it.isNotEmpty() },
                                    includeInTotal,
                                    selectedCurrency,
                                    selectedTheme.templateId,
                                )
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完成",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // 分段选择器：储蓄卡 / 资产 vs 信用卡 / 负债
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(2.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    AccountTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .then(
                                    if (isSelected) {
                                        Modifier.shadow(
                                            elevation = 1.dp,
                                            shape = RoundedCornerShape(7.dp),
                                            ambientColor = Color.Black.copy(alpha = 0.08f),
                                            spotColor = Color.Black.copy(alpha = 0.08f),
                                        )
                                    } else Modifier,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (selectedTab != tab) {
                                        selectedTab = tab
                                        // 智能切换账户名称后缀
                                        if (tab == AccountTab.ASSET && name.endsWith("信用卡")) {
                                            name = name.removeSuffix("信用卡") + "储蓄卡"
                                        } else if (tab == AccountTab.DEBT && name.endsWith("储蓄卡")) {
                                            name = name.removeSuffix("储蓄卡") + "信用卡"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 表单卡片 1：总资产开关、开户机构、账户名称、卡号尾号、结算币种
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. 计入总资产看板
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "计入总资产看板",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = includeInTotal,
                            onCheckedChange = { includeInTotal = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    // 2. 开户机构
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "开户机构",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .clickable { isInstitutionMenuExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = selectedInstitution,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "选择开户机构",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = isInstitutionMenuExpanded,
                                onDismissRequest = { isInstitutionMenuExpanded = false },
                            ) {
                                DefaultInstitutions.forEach { inst ->
                                    DropdownMenuItem(
                                        text = { Text(inst, fontSize = 14.sp) },
                                        onClick = {
                                            val oldInst = selectedInstitution
                                            selectedInstitution = inst
                                            isInstitutionMenuExpanded = false

                                            // 智能同步默认账户名称
                                            val oldSuffix = if (selectedTab == AccountTab.ASSET) "储蓄卡" else "信用卡"
                                            if (name.isBlank() || name == "$oldInst$oldSuffix" || name == oldInst) {
                                                name = if (inst == "微信支付" || inst == "支付宝" || inst == "现金") {
                                                    inst
                                                } else {
                                                    "$inst$oldSuffix"
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    // 3. 账户名称
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "账户名称",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Box(
                            modifier = Modifier
                                .widthIn(min = 120.dp, max = 220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (name.isEmpty()) {
                                        Text(
                                            text = "输入名称",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    innerTextField()
                                },
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    // 4. 卡号尾号
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "卡号尾号",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "· · · ·",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicTextField(
                                    value = cardNumber,
                                    onValueChange = { input ->
                                        if (input.length <= 4 && input.all { it.isDigit() }) {
                                            cardNumber = input
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        if (cardNumber.isEmpty()) {
                                            Text(
                                                text = "6821",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                        innerTextField()
                                    },
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    // 5. 结算币种
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "结算币种",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable { isCurrencyMenuExpanded = true }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = selectedCurrencyDisplay,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    imageVector = Icons.Default.UnfoldMore,
                                    contentDescription = "选择币种",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = isCurrencyMenuExpanded,
                                onDismissRequest = { isCurrencyMenuExpanded = false },
                            ) {
                                DefaultCurrencies.forEach { (code, display) ->
                                    DropdownMenuItem(
                                        text = { Text(display, fontSize = 14.sp) },
                                        onClick = {
                                            selectedCurrency = code
                                            selectedCurrencyDisplay = display
                                            isCurrencyMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 表单卡片 2：当前可用余额
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "当前可用余额",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "¥",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Box(
                            modifier = Modifier
                                .widthIn(min = 90.dp, max = 150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            BasicTextField(
                                value = balanceText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        balanceText = input
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (balanceText.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    innerTextField()
                                },
                            )
                        }
                    }
                }
            }

            if (!isBalanceValid) {
                Text(
                    "请输入有效余额，金额不能超出支持范围",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 表单卡片 3：卡面主题色
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "卡面主题色",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { isThemeMenuExpanded = true }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(selectedTheme.dotColor),
                            )

                            Text(
                                text = selectedTheme.name,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Icon(
                                imageVector = Icons.Default.UnfoldMore,
                                contentDescription = "选择主题",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = isThemeMenuExpanded,
                            onDismissRequest = { isThemeMenuExpanded = false },
                        ) {
                            DefaultThemeOptions.forEach { theme ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(theme.dotColor),
                                        )
                                    },
                                    text = { Text(theme.name, fontSize = 14.sp) },
                                    onClick = {
                                        selectedThemeName = theme.name
                                        isThemeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 表单卡片 4：添加备注与账户用途
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isNoteExpanded = !isNoteExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (note.isBlank()) "添加备注与账户用途..." else note,
                            fontSize = 14.sp,
                            color = if (note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "详细信息",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "展开备注",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isNoteExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        ) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            BasicTextField(
                                value = note,
                                onValueChange = { note = it },
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(10.dp),
                                decorationBox = { innerTextField ->
                                    if (note.isEmpty()) {
                                        Text(
                                            text = "请输入备注或账户用途说明...",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    innerTextField()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

@Preview(showBackground = true)
@Composable
private fun AddAccountSheetContentPreview() {
    androidx.compose.material3.MaterialTheme {
        AddAccountSheetContent(
            onDismissRequest = {},
            onConfirm = { _, _, _, _, _, _, _ -> },
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        )
    }
}

