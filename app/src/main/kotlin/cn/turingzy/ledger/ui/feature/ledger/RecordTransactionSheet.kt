package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import cn.turingzy.ledger.util.toFormattedAmount
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon as ModelIcon
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.util.AmountFormatter

data class QuickCategoryItem(
    val name: String,
    val icon: ImageVector,
    val type: TransactionType,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionSheet(
    sheetState: SheetState,
    categories: List<CategoryWithIcon>,
    accounts: List<AccountWithTemplate>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        type: TransactionType,
        amount: Long,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long,
        note: String?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expression by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    // 默认选择支出首项 "餐饮"
    var selectedCategoryName by rememberSaveable { mutableStateOf("餐饮") }
    var selectedType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }

    var selectedAccountId by rememberSaveable {
        mutableLongStateOf(accounts.firstOrNull()?.account?.id ?: 0L)
    }

    LaunchedEffect(accounts) {
        if (accounts.none { it.account.id == selectedAccountId }) {
            selectedAccountId = accounts.firstOrNull()?.account?.id ?: 0L
        }
    }

    var validationMessage by remember { mutableStateOf<String?>(null) }

    // 计算是否有正在输入的算式运算符 (+ 或 -)
    val hasPendingCalculation = remember(expression) {
        expression.drop(1).any { it == '+' || it == '-' }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        },
        modifier = modifier,
    ) {
        RecordTransactionContent(
            expression = expression,
            note = note,
            selectedCategoryName = selectedCategoryName,
            selectedAccountId = selectedAccountId,
            hasPendingCalculation = hasPendingCalculation,
            accounts = accounts,
            onExpressionChange = {
                expression = it
                validationMessage = null
            },
            onNoteChange = { note = it },
            onCategorySelect = { item ->
                selectedCategoryName = item.name
                selectedType = item.type
                validationMessage = null
            },
            onAccountSelect = {
                selectedAccountId = it
                validationMessage = null
            },
            selectedType = selectedType,
            validationMessage = validationMessage,
            onConfirmAction = confirm@{
                val evaluated = evaluateMathExpression(expression)
                val amountCents = AmountFormatter.parseToCents(evaluated)
                if (amountCents == null) {
                    validationMessage = "请输入有效金额，金额不能超出支持范围"
                    return@confirm
                }
                if (hasPendingCalculation) {
                    expression = evaluated
                    validationMessage = null
                    return@confirm
                }
                if (amountCents <= 0L) {
                    validationMessage = "记账金额需大于 0"
                    return@confirm
                }
                if (accounts.none { it.account.id == selectedAccountId }) {
                    validationMessage = "请先在钱包中添加账户"
                    return@confirm
                }
                val categoryType = when (selectedType) {
                    TransactionType.EXPENSE -> CategoryType.EXPENSE
                    TransactionType.INCOME -> CategoryType.INCOME
                    TransactionType.TRANSFER -> CategoryType.TRANSFER
                }
                val matchingCategories = categories.filter { it.category.type == categoryType }
                val matchedCatId = matchingCategories.firstOrNull {
                    it.category.name.contains(selectedCategoryName) || selectedCategoryName.contains(it.category.name)
                }?.category?.id ?: matchingCategories.firstOrNull()?.category?.id
                if (matchedCatId == null) {
                    validationMessage = "请先添加对应的分类"
                    return@confirm
                }
                val toAccId = accounts.firstOrNull { it.account.id != selectedAccountId }?.account?.id
                if (selectedType == TransactionType.TRANSFER && toAccId == null) {
                    validationMessage = "转账需要至少两个账户"
                    return@confirm
                }
                onConfirm(
                    selectedType,
                    amountCents,
                    selectedAccountId,
                    if (selectedType == TransactionType.TRANSFER) toAccId else null,
                    matchedCatId,
                    note.trim().takeIf { it.isNotBlank() },
                )
            },
        )
    }
}

/**
 * 抽屉式记一笔面板核心内容
 */
@Composable
fun RecordTransactionContent(
    expression: String,
    note: String,
    selectedCategoryName: String,
    selectedAccountId: Long,
    hasPendingCalculation: Boolean,
    accounts: List<AccountWithTemplate>,
    onExpressionChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCategorySelect: (QuickCategoryItem) -> Unit,
    onAccountSelect: (Long) -> Unit,
    onConfirmAction: () -> Unit,
    modifier: Modifier = Modifier,
    selectedType: TransactionType = TransactionType.EXPENSE,
    validationMessage: String? = null,
) {
    val focusManager = LocalFocusManager.current
    val expressionScrollState = rememberScrollState()
    LaunchedEffect(expression) { expressionScrollState.animateScrollTo(expressionScrollState.maxValue) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 金额计算表达式实时显示区域 (如: ¥ 1+5)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "¥ ",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = expression.ifEmpty { "0" },
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f).horizontalScroll(expressionScrollState),
                style = TextStyle(fontFeatureSettings = "tnum"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp,
            )
        }

        if (validationMessage != null) {
            Text(
                text = validationMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        // 分类选择区域: 支出、收入、转账分三行，每行6个图标，左侧带二号字标题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 第 1 行: 支出 (6个图标)
            CategoryRowSection(
                title = "支出",
                items = listOf(
                    QuickCategoryItem("餐饮", Icons.Default.Restaurant, TransactionType.EXPENSE),
                    QuickCategoryItem("交通", Icons.Default.DirectionsCar, TransactionType.EXPENSE),
                    QuickCategoryItem("购物", Icons.Default.ShoppingCart, TransactionType.EXPENSE),
                    QuickCategoryItem("居家", Icons.Default.Home, TransactionType.EXPENSE),
                    QuickCategoryItem("娱乐", Icons.Default.SportsEsports, TransactionType.EXPENSE),
                    QuickCategoryItem("更多", Icons.Default.MoreHoriz, TransactionType.EXPENSE),
                ),
                selectedType = selectedType,
                selectedCategory = selectedCategoryName,
                onSelect = onCategorySelect,
            )

            // 第 2 行: 收入 (6个图标)
            CategoryRowSection(
                title = "收入",
                items = listOf(
                    QuickCategoryItem("工资", Icons.Default.Payments, TransactionType.INCOME),
                    QuickCategoryItem("兼职", Icons.Default.Work, TransactionType.INCOME),
                    QuickCategoryItem("理财", Icons.AutoMirrored.Filled.TrendingUp, TransactionType.INCOME),
                    QuickCategoryItem("礼金", Icons.Default.CardGiftcard, TransactionType.INCOME),
                    QuickCategoryItem("其他", Icons.Default.AccountBalanceWallet, TransactionType.INCOME),
                    QuickCategoryItem("更多", Icons.Default.MoreHoriz, TransactionType.INCOME),
                ),
                selectedType = selectedType,
                selectedCategory = selectedCategoryName,
                onSelect = onCategorySelect,
            )

            // 第 3 行: 转账 (6个图标)
            CategoryRowSection(
                title = "转账",
                items = listOf(
                    QuickCategoryItem("银行卡", Icons.Default.CreditCard, TransactionType.TRANSFER),
                    QuickCategoryItem("微信", Icons.Default.AccountBalance, TransactionType.TRANSFER),
                    QuickCategoryItem("支付宝", Icons.Default.Wallet, TransactionType.TRANSFER),
                    QuickCategoryItem("储蓄", Icons.Default.Savings, TransactionType.TRANSFER),
                    QuickCategoryItem("现金", Icons.Default.Paid, TransactionType.TRANSFER),
                    QuickCategoryItem("更多", Icons.Default.MoreHoriz, TransactionType.TRANSFER),
                ),
                selectedType = selectedType,
                selectedCategory = selectedCategoryName,
                onSelect = onCategorySelect,
            )
        }

        // 备注输入框 (浅灰胶囊背景 + 铅笔图标 + 提示文字)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "备注",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )

                BasicTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (note.isEmpty()) {
                            Text(
                                text = "添加备注 (如: 晚餐便当)...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    },
                )
            }
        }

        // 账户选择与日期信息条 (如: 招商银行卡 ▾    今天)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val currentAccount = accounts.firstOrNull { it.account.id == selectedAccountId }?.account
            val accountDisplayName = currentAccount?.name ?: "请先添加账户"

            Box(modifier = Modifier.weight(1f, fill = false)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = accounts.isNotEmpty()) {
                            focusManager.clearFocus()
                            accountMenuExpanded = true
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = accountDisplayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "▾",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false },
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.account.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingIcon = {
                                if (acc.account.id == selectedAccountId) {
                                    Icon(Icons.Default.Check, contentDescription = "已选择", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                onAccountSelect(acc.account.id)
                                accountMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Text(
                text = "今天",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 内置数字小键盘 (4x4 布局，带加减运算与等号/完成状态动态切换)
        BuiltInKeypad(
            hasPendingCalculation = hasPendingCalculation,
            onNumberClick = { digit ->
                focusManager.clearFocus()
                val lastNumber = expression.split('+', '-').last()
                val decimalPlaces = lastNumber.substringAfter('.', "").length
                if (expression.length < 80 && (!lastNumber.contains('.') || decimalPlaces < 2)) {
                    onExpressionChange(expression + digit)
                }
            },
            onDotClick = {
                focusManager.clearFocus()
                val lastNumberPart = expression.split('+', '-').lastOrNull() ?: ""
                if (!lastNumberPart.contains('.')) {
                    onExpressionChange(expression + if (lastNumberPart.isEmpty()) "0." else ".")
                }
            },
            onOperatorClick = { op ->
                focusManager.clearFocus()
                if (expression.isNotEmpty()) {
                    val lastChar = expression.last()
                    if (lastChar == '+' || lastChar == '-') {
                        onExpressionChange(expression.dropLast(1) + op)
                    } else {
                        onExpressionChange(expression + op)
                    }
                }
            },
            onBackspaceClick = {
                focusManager.clearFocus()
                if (expression.isNotEmpty()) {
                    onExpressionChange(expression.dropLast(1))
                }
            },
            onActionClick = {
                focusManager.clearFocus()
                onConfirmAction()
            },
        )
    }
}

/**
 * 分类行组件：左上角二号字分类名 + 6个等间距图标
 */
@Composable
fun CategoryRowSection(
    title: String,
    items: List<QuickCategoryItem>,
    selectedCategory: String,
    onSelect: (QuickCategoryItem) -> Unit,
    modifier: Modifier = Modifier,
    selectedType: TransactionType = TransactionType.EXPENSE,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // 顶部左侧具体类别二号字
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = item.name == selectedCategory && item.type == selectedType
                val backgroundColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerLow,
                    animationSpec = tween(160), label = "categoryBackground",
                )
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(item) })
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(backgroundColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Text(
                        text = item.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 内置数字小键盘
 * 4 列 x 4 行，包含 1-9, ., 0, ⌫, +, -, 与跨两行的绿色动作按钮 (计算状态显示 "="，非计算状态显示 "完成")
 */
@Composable
fun BuiltInKeypad(
    hasPendingCalculation: Boolean,
    onNumberClick: (String) -> Unit,
    onDotClick: () -> Unit,
    onOperatorClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyHeight = 48.dp
    val spacing = 8.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        // 第 1 行: 1, 2, 3, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            KeypadButton(text = "1", onClick = { onNumberClick("1") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "2", onClick = { onNumberClick("2") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "3", onClick = { onNumberClick("3") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "+", onClick = { onOperatorClick("+") }, modifier = Modifier.weight(1f).height(keyHeight))
        }

        // 第 2 行: 4, 5, 6, -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            KeypadButton(text = "4", onClick = { onNumberClick("4") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "5", onClick = { onNumberClick("5") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "6", onClick = { onNumberClick("6") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "-", onClick = { onOperatorClick("-") }, modifier = Modifier.weight(1f).height(keyHeight))
        }

        // 第 3 & 4 行组合区 (前三列放置 7,8,9 和 .,0,⌫; 第四列为跨两行的大绿色动作按钮)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            // 前三列 (7,8,9 和 .,0,⌫)
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                // 第 3 行前三列: 7, 8, 9
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    KeypadButton(text = "7", onClick = { onNumberClick("7") }, modifier = Modifier.weight(1f).height(keyHeight))
                    KeypadButton(text = "8", onClick = { onNumberClick("8") }, modifier = Modifier.weight(1f).height(keyHeight))
                    KeypadButton(text = "9", onClick = { onNumberClick("9") }, modifier = Modifier.weight(1f).height(keyHeight))
                }

                // 第 4 行前三列: ., 0, ⌫
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    KeypadButton(text = ".", onClick = onDotClick, modifier = Modifier.weight(1f).height(keyHeight))
                    KeypadButton(text = "0", onClick = { onNumberClick("0") }, modifier = Modifier.weight(1f).height(keyHeight))
                    KeypadIconButton(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        onClick = onBackspaceClick,
                        modifier = Modifier.weight(1f).height(keyHeight),
                    )
                }
            }

            // 第四列: 跨两行的绿色动作按钮 (高 keyHeight * 2 + spacing = 104dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight * 2 + spacing)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                        spotColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                    )
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasPendingCalculation) "=" else "完成",
                    fontSize = if (hasPendingCalculation) 24.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "keyPress")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(interactionSource = interactionSource, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun KeypadIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "keyPress")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(interactionSource = interactionSource, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "删除最后一位",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RecordTransactionContentPreview() {
    RecordTransactionContent(
        expression = "1+5",
        note = "",
        selectedCategoryName = "餐饮",
        selectedAccountId = 1L,
        hasPendingCalculation = true,
        accounts = listOf(
            AccountWithTemplate(
                account = Account(id = 1L, name = "招商银行卡", type = AccountType.DEBIT),
                template = null,
            ),
        ),
        onExpressionChange = {},
        onNoteChange = {},
        onCategorySelect = {},
        onAccountSelect = {},
        onConfirmAction = {},
    )
}
