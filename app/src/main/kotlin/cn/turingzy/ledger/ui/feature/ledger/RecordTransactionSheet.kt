package cn.turingzy.ledger.ui.feature.ledger

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
import androidx.compose.ui.graphics.Color
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
import cn.turingzy.ledger.ui.theme.IosCard
import cn.turingzy.ledger.ui.theme.IosGreen
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.IosSecondaryLabel
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
    var expression by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // 默认选择支出首项 "餐饮"
    var selectedCategoryName by remember { mutableStateOf("餐饮") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }

    var selectedAccountId by remember(accounts) {
        mutableLongStateOf(accounts.firstOrNull()?.account?.id ?: 1L)
    }

    // 计算是否有正在输入的算式运算符 (+ 或 -)
    val hasPendingCalculation = remember(expression) {
        expression.drop(1).any { it == '+' || it == '-' }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = IosCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E5EA)),
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
            onExpressionChange = { expression = it },
            onNoteChange = { note = it },
            onCategorySelect = { item ->
                selectedCategoryName = item.name
                selectedType = item.type
            },
            onAccountSelect = { selectedAccountId = it },
            onConfirmAction = {
                if (hasPendingCalculation) {
                    // 处于计算中状态，点击执行求值
                    expression = evaluateMathExpression(expression)
                } else {
                    // 无待计算公式，点击“完成”进行保存
                    val evaluated = evaluateMathExpression(expression)
                    val amountCents = AmountFormatter.parseToCents(evaluated) ?: 0L
                    if (amountCents > 0L) {
                        val matchedCatId = categories.firstOrNull {
                            it.category.name.contains(selectedCategoryName) || selectedCategoryName.contains(it.category.name)
                        }?.category?.id
                            ?: categories.firstOrNull { it.category.type == CategoryType.EXPENSE }?.category?.id
                            ?: 1L

                        val toAccId = accounts.firstOrNull { it.account.id != selectedAccountId }?.account?.id
                        onConfirm(
                            selectedType,
                            amountCents,
                            selectedAccountId,
                            if (selectedType == TransactionType.TRANSFER) toAccId else null,
                            matchedCatId,
                            note.takeIf { it.isNotBlank() },
                        )
                    }
                }
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
) {
    var accountMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
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
                color = IosLabel,
            )
            Text(
                text = expression.ifEmpty { "0" },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = IosLabel,
                letterSpacing = (-0.5).sp,
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
                .background(Color(0xFFF2F2F7))
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
                    tint = IosSecondaryLabel,
                    modifier = Modifier.size(16.dp),
                )

                BasicTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = IosLabel,
                    ),
                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (note.isEmpty()) {
                            Text(
                                text = "添加备注 (如: 晚餐便当)...",
                                fontSize = 13.sp,
                                color = IosSecondaryLabel.copy(alpha = 0.6f),
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
            val accountDisplayName = currentAccount?.name ?: "招商银行卡"

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { accountMenuExpanded = true }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = accountDisplayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = IosSecondaryLabel,
                    )
                    Text(
                        text = "▾",
                        fontSize = 12.sp,
                        color = IosSecondaryLabel,
                    )
                }

                DropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false },
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.account.name) },
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
                color = IosSecondaryLabel,
            )
        }

        // 内置数字小键盘 (4x4 布局，带加减运算与等号/完成状态动态切换)
        BuiltInKeypad(
            hasPendingCalculation = hasPendingCalculation,
            onNumberClick = { digit ->
                onExpressionChange(expression + digit)
            },
            onDotClick = {
                val lastNumberPart = expression.split('+', '-').lastOrNull() ?: ""
                if (!lastNumberPart.contains('.')) {
                    onExpressionChange(expression + if (lastNumberPart.isEmpty()) "0." else ".")
                }
            },
            onOperatorClick = { op ->
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
                if (expression.isNotEmpty()) {
                    onExpressionChange(expression.dropLast(1))
                }
            },
            onActionClick = onConfirmAction,
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
            color = IosSecondaryLabel,
            modifier = Modifier.padding(start = 2.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = item.name == selectedCategory
                Column(
                    modifier = Modifier
                        .clickable { onSelect(item) }
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) IosGreen else Color(0xFFF2F2F7))
                            .then(
                                if (isSelected) {
                                    Modifier.shadow(
                                        elevation = 4.dp,
                                        shape = CircleShape,
                                        ambientColor = IosGreen.copy(alpha = 0.35f),
                                        spotColor = IosGreen.copy(alpha = 0.35f),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = if (isSelected) Color.White else Color(0xFF3C3C43),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Text(
                        text = item.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) IosGreen else IosSecondaryLabel,
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
                    .background(IosGreen)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = IosGreen.copy(alpha = 0.35f),
                        spotColor = IosGreen.copy(alpha = 0.35f),
                    )
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasPendingCalculation) "=" else "完成",
                    fontSize = if (hasPendingCalculation) 24.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F7))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = IosLabel,
        )
    }
}

@Composable
fun KeypadIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F7))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IosLabel,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * 基础算式安全求值 (支持 + 与 - 连续计算，如 1+5=6, 10.5-2.5=8)
 */
fun evaluateMathExpression(expr: String): String {
    val trimmed = expr.trim()
    if (trimmed.isEmpty()) return ""

    return try {
        var result = 0.0
        var currentToken = ""
        var currentOperator = '+'

        for (i in trimmed.indices) {
            val char = trimmed[i]
            if (char.isDigit() || char == '.') {
                currentToken += char
            } else if (char == '+' || char == '-') {
                if (currentToken.isNotEmpty()) {
                    val num = currentToken.toDoubleOrNull() ?: 0.0
                    result = if (currentOperator == '+') result + num else result - num
                    currentToken = ""
                }
                currentOperator = char
            }
        }

        if (currentToken.isNotEmpty()) {
            val num = currentToken.toDoubleOrNull() ?: 0.0
            result = if (currentOperator == '+') result + num else result - num
        }

        if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", result).trimEnd('0').trimEnd('.')
        }
    } catch (_: Exception) {
        trimmed
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
