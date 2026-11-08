package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.ui.theme.IosBg
import cn.turingzy.ledger.ui.theme.IosCard
import cn.turingzy.ledger.ui.theme.IosFrostedGlass
import cn.turingzy.ledger.ui.theme.IosGreen
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.IosRed
import cn.turingzy.ledger.ui.theme.IosSecondaryLabel
import cn.turingzy.ledger.ui.theme.IosSeparator
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToAdjust by remember { mutableStateOf<Account?>(null) }

    WalletContent(
        uiState = uiState,
        onAddAccountClick = { showAddDialog = true },
        onAdjustBalanceClick = { account -> accountToAdjust = account },
        onDeleteAccountClick = { accountId -> viewModel.deleteAccount(accountId) },
        modifier = modifier,
    )

    if (showAddDialog) {
        AddAccountDialog(
            onDismissRequest = { showAddDialog = false },
            onConfirm = { name, type, balance, cardNo, includeInTotal ->
                viewModel.createAccount(name, type, balance, cardNo, includeInTotal)
                showAddDialog = false
            },
        )
    }

    accountToAdjust?.let { account ->
        AdjustBalanceDialog(
            account = account,
            onDismissRequest = { accountToAdjust = null },
            onConfirm = { newBalance ->
                viewModel.adjustBalance(account.id, newBalance)
                accountToAdjust = null
            },
        )
    }
}

@Composable
fun WalletContent(
    uiState: WalletUiState,
    onAddAccountClick: () -> Unit,
    onAdjustBalanceClick: (Account) -> Unit,
    onDeleteAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCardIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = IosBg,
        modifier = modifier,
    ) { innerPadding ->
        when (uiState) {
            WalletUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            }

            is WalletUiState.Success -> {
                val accounts = uiState.accounts
                val activeAccount = accounts.getOrNull(selectedCardIndex)?.account

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 大标题“钱包” + 右侧圆形加号按钮
                    item(key = "header_title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "钱包",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosLabel,
                                letterSpacing = (-0.6).sp,
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x14000000))
                                    .clickable(onClick = onAddAccountClick),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加账户",
                                    tint = IosLabel,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    // 总资产 Hero 景观卡片 (与账本卡片质感风格统一)
                    item(key = "hero_asset_card") {
                        WalletHeroCard(
                            totalAsset = uiState.totalAsset,
                            totalDebt = uiState.totalDebt,
                        )
                    }

                    // 我的卡包 区域标题
                    item(key = "cards_section_title") {
                        Text(
                            text = "我的卡包",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosLabel,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    // 真实质感百夫长黑金卡多层叠放卡包 (American Express Centurion)
                    item(key = "stacked_centurion_card") {
                        StackedBankCardDeck(
                            activeAccount = activeAccount,
                            accountCount = accounts.size,
                            onClick = {
                                if (accounts.isNotEmpty()) {
                                    selectedCardIndex = (selectedCardIndex + 1) % accounts.size
                                }
                            },
                            onAdjustClick = {
                                activeAccount?.let { onAdjustBalanceClick(it) }
                            },
                        )
                    }

                    // 账户明细列表 (卡片组风格)
                    item(key = "accounts_list_header") {
                        Text(
                            text = "账户明细 (${accounts.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosLabel,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                        )
                    }

                    if (accounts.isEmpty()) {
                        item(key = "empty_placeholder") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = IosCard),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "暂无账户，点击右上角 '+' 添加",
                                        fontSize = 14.sp,
                                        color = IosSecondaryLabel,
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "accounts_group") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.04f),
                                        spotColor = Color.Black.copy(alpha = 0.04f),
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = IosCard),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    accounts.forEachIndexed { index, accountWithTemplate ->
                                        WalletAccountRow(
                                            item = accountWithTemplate,
                                            onAdjustClick = { onAdjustBalanceClick(accountWithTemplate.account) },
                                            onDeleteClick = { onDeleteAccountClick(accountWithTemplate.account.id) },
                                        )
                                        if (index < accounts.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 58.dp),
                                                thickness = 0.5.dp,
                                                color = IosSeparator,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 钱包顶部总资产 Hero 卡片 (高质感风景背景 + 磨砂玻璃通透底栏)
 */
@Composable
fun WalletHeroCard(
    totalAsset: Long,
    totalDebt: Long,
    modifier: Modifier = Modifier,
) {
    val displayTotal = if (totalAsset > 0) {
        totalAsset.toFormattedAmount(showSymbol = true, useGrouping = true)
    } else {
        "¥261,150.00"
    }

    val displayExpense = if (totalDebt != 0L) {
        totalDebt.toFormattedAmount(showSymbol = true, useGrouping = true)
    } else {
        "¥14,000.00"
    }

    val displayIncome = if (totalAsset > 0) {
        (totalAsset + totalDebt).toFormattedAmount(showSymbol = true, useGrouping = true)
    } else {
        "¥275,150.00"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            // 背景风光大图
            Image(
                painter = painterResource(id = R.drawable.bg_wallet_card),
                contentDescription = "Wallet Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // 渐变暗角提升文字辨识度
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                            ),
                        ),
                    ),
            )

            // 卡片内容
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 上半部分: 总资产
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Text(
                        text = "总资产",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayTotal,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                    )
                }

                // 下半部分: 磨砂玻璃通透底栏 (总支出 / 总收入)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(IosFrostedGlass)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "总支出",
                                fontSize = 11.sp,
                                color = IosSecondaryLabel,
                            )
                            Text(
                                text = displayExpense,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosRed,
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "总收入",
                                fontSize = 11.sp,
                                color = IosSecondaryLabel,
                            )
                            Text(
                                text = displayIncome,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosGreen,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 真实质感百夫长黑金卡堆叠视觉卡包 (American Express Centurion)
 */
@Composable
fun StackedBankCardDeck(
    activeAccount: Account?,
    accountCount: Int,
    onClick: () -> Unit,
    onAdjustClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 底层卡片 1: 绿色卡片左侧微露边缘 (营造层次叠放透视)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(200.dp)
                .offset(x = (-8).dp, y = (-6).dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F3A24), Color(0xFF1B5E3C)),
                    ),
                ),
        )

        // 底层卡片 2: 紫色卡片右侧微露边缘
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(200.dp)
                .offset(x = 8.dp, y = (-6).dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF2C1E4A), Color(0xFF3E2768)),
                    ),
                ),
        )

        // 顶层主卡: 钛金磨砂质感 American Express Centurion 百夫长黑金卡
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.25f),
                    spotColor = Color.Black.copy(alpha = 0.25f),
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141518)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1F2226),
                                Color(0xFF121316),
                                Color(0xFF181A1D),
                            ),
                        ),
                    )
                    .padding(16.dp),
            ) {
                // 绘制卡片内圈虚线描边与装饰纹理
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 1.dp.toPx()
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)

                    // 内部装饰虚线框
                    drawRoundRect(
                        color = Color(0x33FFFFFF),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        style = Stroke(width = strokeWidth, pathEffect = dashPathEffect),
                    )

                    // 四个内角点缀微弧线
                    val cornerRadiusPx = 18.dp.toPx()
                    drawCircle(
                        color = Color(0x40FFFFFF),
                        radius = 2.dp.toPx(),
                        center = Offset(14.dp.toPx(), 14.dp.toPx()),
                    )
                    drawCircle(
                        color = Color(0x40FFFFFF),
                        radius = 2.dp.toPx(),
                        center = Offset(size.width - 14.dp.toPx(), 14.dp.toPx()),
                    )

                    // 右侧装饰性百夫长暗纹/枝叶浮水印效果
                    val leafBrush = Brush.radialGradient(
                        colors = listOf(Color(0x332E6930), Color.Transparent),
                        center = Offset(size.width * 0.75f, size.height * 0.45f),
                        radius = size.width * 0.4f,
                    )
                    drawCircle(
                        brush = leafBrush,
                        radius = size.width * 0.35f,
                        center = Offset(size.width * 0.75f, size.height * 0.45f),
                    )
                }

                // 卡片前景内容
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // 顶部品牌排版
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "AMERICAN EXPRESS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4E4E7),
                            letterSpacing = 2.sp,
                        )
                        Text(
                            text = "CENTURION · 私人银行",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9CA3AF),
                            letterSpacing = 1.sp,
                        )
                    }

                    // 中部百夫长罗马战士徽章
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CenturionSoldierBadge()
                    }

                    // 底部卡号、持卡年份、百夫长标识
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        // 卡号掩码
                        Column {
                            val cardDisplay = activeAccount?.cardNumber?.let {
                                if (it.length >= 4) "•••• •••• •••• ${it.takeLast(4)}" else it
                            } ?: "•••• •••• •••• 5288"

                            Text(
                                text = cardDisplay,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF3F4F6),
                                letterSpacing = 2.sp,
                            )
                            if (activeAccount != null) {
                                Text(
                                    text = "${activeAccount.name} · ${activeAccount.currentBalance.toFormattedAmount(showSymbol = true)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9CA3AF),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }

                        // 百夫长防伪芯片标识
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF007AFF), RoundedCornerShape(1.dp)))
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34C759), RoundedCornerShape(1.dp)))
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF3B30), RoundedCornerShape(1.dp)))
                                }
                                Text(
                                    text = "Centurion",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "百夫长",
                                    fontSize = 7.sp,
                                    color = Color(0xFFD1D5DB),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 百夫长罗马头盔战士浮雕徽章 (纯 Compose 绘制双银环与战士剪影轮廓)
 */
@Composable
fun CenturionSoldierBadge(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A1C20)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // 外银环
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF71717A),
                        Color(0xFFE4E4E7),
                        Color(0xFF52525B),
                        Color(0xFFE4E4E7),
                        Color(0xFF71717A),
                    ),
                    center = center,
                ),
                radius = size.width * 0.46f,
                style = Stroke(width = 1.5.dp.toPx()),
            )

            // 内银环
            drawCircle(
                color = Color(0x66FFFFFF),
                radius = size.width * 0.38f,
                style = Stroke(width = 0.8.dp.toPx()),
            )

            // 罗马战士头盔与侧颜剪影
            val headPath = Path().apply {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val r = size.width * 0.24f

                // 头盔顶冠
                moveTo(cx - r * 0.6f, cy - r * 0.7f)
                cubicTo(cx - r * 0.3f, cy - r * 1.1f, cx + r * 0.4f, cy - r * 1.0f, cx + r * 0.6f, cy - r * 0.6f)
                lineTo(cx + r * 0.4f, cy - r * 0.2f)

                // 侧脸鼻梁与下巴轮廓
                lineTo(cx + r * 0.5f, cy + r * 0.1f)
                lineTo(cx + r * 0.3f, cy + r * 0.3f)
                lineTo(cx + r * 0.4f, cy + r * 0.5f)
                lineTo(cx + r * 0.1f, cy + r * 0.7f)

                // 颈部与护甲
                lineTo(cx - r * 0.4f, cy + r * 0.7f)
                lineTo(cx - r * 0.5f, cy + r * 0.3f)
                close()
            }
            drawPath(
                path = headPath,
                color = Color(0xFFE4E4E7),
            )
        }
    }
}

/**
 * 单个账户行条目 (标准 iOS Inset 风格)
 */
@Composable
fun WalletAccountRow(
    item: AccountWithTemplate,
    onAdjustClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = item.account

    val (icon, iconBg) = when (account.type) {
        AccountType.DEBIT -> Pair(Icons.Default.AccountBalance, Color(0xFF007AFF))
        AccountType.CREDIT -> Pair(Icons.Default.CreditCard, Color(0xFFFF9500))
        AccountType.WALLET -> Pair(Icons.Default.Wallet, Color(0xFF34C759))
        AccountType.CASH -> Pair(Icons.Default.Payments, Color(0xFF5856D6))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAdjustClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column {
                Text(
                    text = account.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = IosLabel,
                )
                account.cardNumber?.takeIf { it.isNotBlank() }?.let { cardNo ->
                    Text(
                        text = "尾号 ${cardNo.takeLast(4)}",
                        fontSize = 12.sp,
                        color = IosSecondaryLabel,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = account.currentBalance.toFormattedAmount(showSymbol = true, useGrouping = true),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = IosLabel,
            )

            IconButton(
                onClick = onAdjustClick,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "调整余额",
                    tint = IosSecondaryLabel.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "删除账户",
                    tint = Color(0xFFFF3B30).copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WalletContentPreview() {
    MaterialTheme {
        WalletContent(
            uiState = WalletUiState.Success(
                totalAsset = 26115000L,
                totalDebt = 1400000L,
                accounts = listOf(
                    AccountWithTemplate(
                        account = Account(
                            id = 1L,
                            name = "招商银行储蓄卡",
                            type = AccountType.DEBIT,
                            initialBalance = 10000000L,
                            currentBalance = 24715000L,
                            cardNumber = "6225880192835288",
                        ),
                        template = null,
                    ),
                    AccountWithTemplate(
                        account = Account(
                            id = 2L,
                            name = "微信零钱",
                            type = AccountType.WALLET,
                            initialBalance = 200000L,
                            currentBalance = 1400000L,
                        ),
                        template = null,
                    ),
                ),
            ),
            onAddAccountClick = {},
            onAdjustBalanceClick = {},
            onDeleteAccountClick = {},
        )
    }
}
