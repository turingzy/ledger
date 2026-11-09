package cn.turingzy.ledger.ui.feature.wallet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.ui.theme.IosBg
import cn.turingzy.ledger.ui.theme.IosCard
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.IosSecondaryLabel
import cn.turingzy.ledger.util.toFormattedAmount
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var accountToAdjust by remember { mutableStateOf<Account?>(null) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    WalletContent(
        uiState = uiState,
        onAddAccountClick = { showAddSheet = true },
        onAdjustBalanceClick = { account -> accountToAdjust = account },
        modifier = modifier,
    )

    if (showAddSheet) {
        AddAccountSheet(
            sheetState = addSheetState,
            onDismissRequest = {
                scope.launch { addSheetState.hide() }.invokeOnCompletion {
                    if (!addSheetState.isVisible) {
                        showAddSheet = false
                    }
                }
            },
            onConfirm = { name, type, balance, cardNo, includeInTotal, currency, templateId ->
                viewModel.createAccount(
                    name = name,
                    type = type,
                    initialBalance = balance,
                    cardNumber = cardNo,
                    includeInTotal = includeInTotal,
                    currency = currency,
                    templateId = templateId,
                )
                scope.launch { addSheetState.hide() }.invokeOnCompletion {
                    if (!addSheetState.isVisible) {
                        showAddSheet = false
                    }
                }
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
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") onDeleteAccountClick: (Long) -> Unit = {},
) {
    Scaffold(
        containerColor = IosBg,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) { innerPadding ->
        when (uiState) {
            WalletUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = IosLabel)
                }
            }

            is WalletUiState.Success -> {
                val accounts = uiState.accounts
                var isListView by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 0.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 与账本页面顶栏设计保持一致: 大标题“钱包” + 右侧添加按钮 + 资产卡片 (卡片与顶栏间距 5.dp)
                    item(key = "header_and_hero") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "钱包",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IosLabel,
                                        letterSpacing = (-0.6).sp,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                    )

                                    // 添加按钮 (与账本顶栏图标按钮风格统一: 圆形水波纹反馈，无底色，IosSecondaryLabel 颜色，尺寸 24.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true, radius = 18.dp),
                                            ) {
                                                onAddAccountClick()
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "添加账户",
                                            tint = IosSecondaryLabel,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }

                            // 资产卡片 (与账本卡片质感风格保持绝对一致：专属底图 + 毛玻璃通透底栏总支出与总收入)
                            WalletHeroCard(
                                totalAsset = uiState.totalAsset,
                                totalDebt = uiState.totalDebt,
                            )
                        }
                    }

                    // 我的卡包 区域标题 + 右侧视图切换按钮 (堆叠视图 / 垂直平铺列表视图)
                    item(key = "cards_section_title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "我的卡包",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosSecondaryLabel,
                            )

                            // 视图切换按钮 (默认卡片堆叠视图，点击切换为垂直平铺列表视图)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, radius = 16.dp),
                                    ) {
                                        isListView = !isListView
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isListView) Icons.Default.ViewCarousel else Icons.Default.ViewAgenda,
                                    contentDescription = if (isListView) "切换为卡片堆叠视图" else "切换为列表视图",
                                    tint = IosSecondaryLabel,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }

                    if (!isListView) {
                        // 1. 卡片堆叠视图 (默认)
                        item(key = "stacked_bank_cards") {
                            SwipeableBankCardDeck(
                                accounts = accounts,
                                onAdjustClick = onAdjustBalanceClick,
                            )
                        }
                    } else {
                        // 2. 卡片列表视图：垂直平铺流式布局 (Vertical Stacked List / Expanded List View)
                        // 所有银行卡纵向垂直平铺依次展开，卡片宽度与上方资产卡片等宽对齐，高度收敛为两行内容
                        if (accounts.isEmpty()) {
                            item(key = "empty_cards_list") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(76.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = IosCard),
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "暂无卡片，点击右上角 '+' 添加",
                                            fontSize = 14.sp,
                                            color = IosSecondaryLabel,
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = accounts,
                                key = { "compact_${it.account.id}" },
                            ) { item ->
                                CompactBankCardItem(
                                    account = item.account,
                                    template = item.template,
                                    onAdjustClick = { onAdjustBalanceClick(item.account) },
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
 * 钱包顶部总资产 Hero 卡片 (与账本卡片质感风格保持绝对一致：专属底图 + 磨砂玻璃通透底栏)
 * 底部透明层展示：左侧红色总支出、右侧绿色总收入
 */
@Composable
fun WalletHeroCard(
    totalAsset: Long,
    totalDebt: Long,
    modifier: Modifier = Modifier,
) {
    val cardHeight = 152.dp
    val glassPanelHeight = 52.dp
    val context = LocalContext.current

    val blurredBackdrop = remember(context) {
        try {
            val original = BitmapFactory.decodeResource(context.resources, R.drawable.bg_wallet_card)
            if (original != null) {
                val scaled = Bitmap.createScaledBitmap(original, 240, 120, true)
                fastBlurBitmap(scaled, radius = 4).asImageBitmap()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        // 全局底层清晰风景背景图 (完整呈现底图风光，采用 bg_wallet_card)
        Image(
            painter = painterResource(R.drawable.bg_wallet_card),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, -0.2f),
            modifier = Modifier.fillMaxSize(),
        )

        // 上半区通透微弱暗调渐变 (保障金额文字在亮部背景下的清晰可读性)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x380F172A),
                            Color(0x0A0F172A),
                            Color(0x1A0F172A),
                        ),
                    ),
                ),
        )

        // 上半部: 总资产展示 (严格与下方总支出对齐在 18.dp 左内边距基准线)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp),
        ) {
            Text(
                text = "总资产",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                style = TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.50f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 6f,
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = totalAsset.toFormattedAmount(showSymbol = true, useGrouping = true),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.6).sp,
                style = TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(0f, 3f),
                        blurRadius = 12f,
                    ),
                ),
            )
        }

        // 下半部: 半透明深色毛玻璃面板 (与账本卡片样式完全一致: 左边红色总支出、右边绿色总收入)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(glassPanelHeight)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
        ) {
            // 1. 背景模糊层
            if (blurredBackdrop != null) {
                Image(
                    bitmap = blurredBackdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, 0.7f),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.bg_wallet_card),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, -0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight + 40.dp)
                        .offset(y = -(cardHeight - glassPanelHeight) - 20.dp)
                        .blur(
                            radius = 20.dp,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded,
                        ),
                )
            }

            // 2. 通透深色半透明滤镜 (与账本卡片一致)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x590E222D),
                                Color(0x730C1E27),
                                Color(0x8C09161D),
                            ),
                        ),
                    ),
            )

            // 3. 毛玻璃顶部微光分割线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.32f)),
            )

            // 4. 下半部内容：总支出与总收入 (严格对齐 prototype.html: 左边红色总支出、右边绿色总收入)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 总支出：左对齐 (卡片背景 2号字，语义红色)
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "总支出",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.70f),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.70f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 4f,
                            ),
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = totalDebt.toFormattedAmount(showSymbol = true, useGrouping = true),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF453A),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.75f),
                                offset = Offset(0f, 2f),
                                blurRadius = 6f,
                            ),
                        ),
                    )
                }

                // 总收入：右对齐 (卡片背景 2号字，语义绿色)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "总收入",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.70f),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.70f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 4f,
                            ),
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val totalIncome = totalAsset + totalDebt
                    Text(
                        text = totalIncome.toFormattedAmount(showSymbol = true, useGrouping = true),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF30D158),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.75f),
                                offset = Offset(0f, 2f),
                                blurRadius = 6f,
                            ),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * 银行卡 3D 层叠循环轮播卡包 (Card Deck Infinite Carousel)
 * - 宽度对齐 (Width Alignment)：多张卡片堆叠时的总体宽度，以及单张卡片展开时的宽度，严格与上方“资产卡片”等宽对齐
 * - 堆叠视觉 (Stacked Visuals)：未选中的底层卡片在主卡两侧微微漏出边缘，形成递进深度感
 * - 交互方式 (Interaction)：中央主卡支持点击；基于原生 HorizontalPager 实现左右无缝循环滑动翻阅 (Infinite Carousel)，滑动到头自动循环，永无尽头
 * - 只有两张卡片时：第一张卡片的左右两翼均为第二张卡片，无缝对称循环
 * - 纯实体质感：卡片全实体不透明 (alpha = 1f)，绝无半透明透视；采用原生 120fps 弹性缓动，自然流畅无任何卡顿
 */
@Composable
fun SwipeableBankCardDeck(
    accounts: List<AccountWithTemplate>,
    onAdjustClick: (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accounts.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(194.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = IosCard),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无卡片，点击右上角 '+' 添加",
                    fontSize = 14.sp,
                    color = IosSecondaryLabel,
                )
            }
        }
        return
    }

    val n = accounts.size
    val coroutineScope = rememberCoroutineScope()

    if (n == 1) {
        // 单张卡片展开时的宽度，严格与上方“资产卡片”保持等宽对齐
        val item = accounts[0]
        BankCardItem(
            account = item.account,
            template = item.template,
            onAdjustClick = { onAdjustClick(item.account) },
            modifier = modifier
                .fillMaxWidth()
                .height(194.dp),
        )
    } else {
        // 多张卡片堆叠时：基于 HorizontalPager 实现原生 120fps 顺滑无缝循环
        // initialPage 取大数并整除 n，确保初始卡片为 accounts[0]，且向左向右均可无限滑动
        val initialPage = ((10000 / n) / 2) * n
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { 10000 },
        )

        val peekWidth = 14.dp

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = peekWidth),
            pageSpacing = 0.dp,
            modifier = modifier
                .fillMaxWidth()
                .height(202.dp),
        ) { page ->
            val accountWithTemplate = accounts[page % n]

            BankCardItem(
                account = accountWithTemplate.account,
                template = accountWithTemplate.template,
                onAdjustClick = {
                    if (page == pagerState.currentPage) {
                        onAdjustClick(accountWithTemplate.account)
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(194.dp)
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                        val scale = 1f - (0.06f * absOffset)
                        val originX = if (pageOffset > 0f) 1f else if (pageOffset < 0f) 0f else 0.5f

                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(originX, 0.5f)
                        shadowElevation = (10f - absOffset * 6f).dp.toPx()
                        alpha = 1f
                    },
            )
        }
    }
}

/**
 * 单张实体质感银行卡卡面 (对齐 prototype.html 现实银行卡物理排版细节)
 */
@Composable
fun BankCardItem(
    account: Account,
    @Suppress("UNUSED_PARAMETER") template: CardTemplate?,
    onAdjustClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCenturion = account.templateId == 2L || account.name.contains("百夫长") || account.name.contains("运通")

    val cardGradients = when {
        isCenturion -> listOf(Color(0xFF0D0E11), Color(0xFF141519), Color(0xFF08080A))
        account.templateId == 1L -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)) // 经典深蓝
        account.templateId == 3L -> listOf(Color(0xFF075E37), Color(0xFF03361E)) // 翡翠绿 (微信/活力薄荷)
        account.type == AccountType.CREDIT -> listOf(Color(0xFF2D143D), Color(0xFF1A0D26)) // 尊贵紫 (白金信用卡)
        account.type == AccountType.WALLET && account.name.contains("支付宝") -> listOf(Color(0xFF0A438C), Color(0xFF052148)) // 皇家科技蓝
        else -> listOf(Color(0xFF1C1C1E), Color(0xFF121214)) // 黑曜金 (金葵花/储蓄卡)
    }

    Card(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .clickable(onClick = onAdjustClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardGradients.first()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(cardGradients))
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 顶部行：
                if (isCenturion) {
                    // 百夫长黑金卡专用纯正雕刻标题 (对齐 prototype.html)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "AMERICAN EXPRESS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEDEDF0),
                            letterSpacing = 2.sp,
                        )
                        Text(
                            text = "CENTURION · 私人银行",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE5E5EA).copy(alpha = 0.65f),
                            letterSpacing = 1.sp,
                        )
                    }
                } else {
                    // 标准银行卡排版：机构品牌与卡种标识 + 芯片与闪付波纹
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = account.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }

                        // 闪付非接触波纹标识
                        ContactlessWaveIcon()
                    }
                }

                // 中间区域：
                if (isCenturion) {
                    // 罗马百夫长战士徽章浮雕
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CenturionSoldierBadge()
                    }
                } else {
                    // 芯片 + 当前余额直观展示
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EmvChip()

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (account.type == AccountType.CREDIT) "当前欠款" else "当前可用余额",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.70f),
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = account.currentBalance.toFormattedAmount(showSymbol = true, useGrouping = true),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp,
                            )
                        }
                    }
                }

                // 底部行：卡号掩码、持卡人姓名、银联芯片徽章 (1:1 还原 prototype.html)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        val cardDisplay = account.cardNumber?.let {
                            if (it.length >= 4) "•••• •••• •••• ${it.takeLast(4)}" else it
                        } ?: "•••• •••• •••• 5288"

                        Text(
                            text = cardDisplay,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.5.sp,
                        )

                        if (!isCenturion) {
                            Text(
                                text = "ZHANG YU · VALID 12/30",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.65f),
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }

                    if (isCenturion) {
                        UnionPayBadge(label = "百夫长")
                    } else {
                        UnionPayBadge(label = "银联")
                    }
                }
            }
        }
    }
}

/**
 * 紧凑银行卡卡面 (卡片列表视图模式专用：两行内容垂直平铺流式布局)
 * - 宽度严格与上方资产卡片等宽对齐 (fillMaxWidth)
 * - 高度紧凑为两行内容 (76.dp)
 * - 第一行：卡片名称 + 账户类型微徽章 (左) | 账户余额 (右)
 * - 第二行：卡号尾号掩码 (左) | 银联/百夫长标识 (右)
 */
@Composable
fun CompactBankCardItem(
    account: Account,
    @Suppress("UNUSED_PARAMETER") template: CardTemplate?,
    onAdjustClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCenturion = account.templateId == 2L || account.name.contains("百夫长") || account.name.contains("运通")

    val cardGradients = when {
        isCenturion -> listOf(Color(0xFF0D0E11), Color(0xFF141519), Color(0xFF08080A))
        account.templateId == 1L -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)) // 经典深蓝
        account.templateId == 3L -> listOf(Color(0xFF075E37), Color(0xFF03361E)) // 翡翠绿 (微信)
        account.type == AccountType.CREDIT -> listOf(Color(0xFF2D143D), Color(0xFF1A0D26)) // 尊贵紫 (白金信用卡)
        account.type == AccountType.WALLET && account.name.contains("支付宝") -> listOf(Color(0xFF0A438C), Color(0xFF052148)) // 皇家科技蓝
        else -> listOf(Color(0xFF1C1C1E), Color(0xFF121214)) // 黑曜金 (金葵花/储蓄卡)
    }

    val typeLabel = when (account.type) {
        AccountType.DEBIT -> "储蓄卡"
        AccountType.CREDIT -> "信用卡"
        AccountType.WALLET -> "电子钱包"
        AccountType.CASH -> "现金"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            )
            .clickable(onClick = onAdjustClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardGradients.first()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(cardGradients))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 第一行 (Line 1): 账户名称 + 类型微胶囊 (左) | 当前余额/欠款 (右)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = account.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.2).sp,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp),
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.88f),
                            )
                        }
                    }

                    Text(
                        text = account.currentBalance.toFormattedAmount(showSymbol = true, useGrouping = true),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.4).sp,
                    )
                }

                // 第二行 (Line 2): 尾号掩码 (左) | 银联/百夫长芯片徽章 (右)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val cardDisplay = account.cardNumber?.let {
                        if (it.length >= 4) "•••• •••• •••• ${it.takeLast(4)}" else it
                    } ?: "•••• •••• •••• 5288"

                    Text(
                        text = cardDisplay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.70f),
                        letterSpacing = 1.sp,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isCenturion) {
                            UnionPayBadge(label = "百夫长")
                        } else {
                            UnionPayBadge(label = "银联")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 实体卡 EMV 金属安全芯片
 */
@Composable
fun EmvChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 32.dp, height = 22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFE082),
                        Color(0xFFFFB300),
                        Color(0xFFFFA000),
                    ),
                ),
            )
            .border(0.8.dp, Color(0xFFFF8F00), RoundedCornerShape(4.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color(0x40000000),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 0.8f,
            )
            drawLine(
                color = Color(0x40000000),
                start = Offset(size.width * 0.35f, 0f),
                end = Offset(size.width * 0.35f, size.height),
                strokeWidth = 0.8f,
            )
            drawLine(
                color = Color(0x40000000),
                start = Offset(size.width * 0.65f, 0f),
                end = Offset(size.width * 0.65f, size.height),
                strokeWidth = 0.8f,
            )
        }
    }
}

/**
 * 银联 UnionPay 芯片徽章
 */
@Composable
fun UnionPayBadge(
    modifier: Modifier = Modifier,
    label: String = "银联",
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier
                .size(width = 15.dp, height = 11.dp)
                .clip(RoundedCornerShape(2.dp)),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFE60012)))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF003882)))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF008453)))
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/**
 * 非接触式闪付波纹标识 (Contactless)
 */
@Composable
fun ContactlessWaveIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val cx = size.width * 0.2f
        val cy = size.height * 0.5f

        val stroke = Stroke(width = 1.4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawArc(
            color = Color.White.copy(alpha = 0.8f),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - 3.dp.toPx(), cy - 3.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 6.dp.toPx()),
            style = stroke,
        )
        drawArc(
            color = Color.White.copy(alpha = 0.8f),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - 6.dp.toPx(), cy - 6.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 12.dp.toPx()),
            style = stroke,
        )
        drawArc(
            color = Color.White.copy(alpha = 0.8f),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - 9.dp.toPx(), cy - 9.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 18.dp.toPx()),
            style = stroke,
        )
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
            .size(46.dp)
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

                moveTo(cx - r * 0.6f, cy - r * 0.7f)
                cubicTo(cx - r * 0.3f, cy - r * 1.1f, cx + r * 0.4f, cy - r * 1.0f, cx + r * 0.6f, cy - r * 0.6f)
                lineTo(cx + r * 0.4f, cy - r * 0.2f)
                lineTo(cx + r * 0.5f, cy + r * 0.1f)
                lineTo(cx + r * 0.3f, cy + r * 0.3f)
                lineTo(cx + r * 0.4f, cy + r * 0.5f)
                lineTo(cx + r * 0.1f, cy + r * 0.7f)
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
 * 盒式卷积模糊算法，用于将下采样位图快速平滑柔化，
 * 在任何设备和 Compose Preview 上百分百复刻 backdrop-filter 散焦质感。
 */
private fun fastBlurBitmap(source: Bitmap, radius: Int): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val outPixels = IntArray(width * height)

    for (y in 0 until height) {
        val yOffset = y * width
        for (x in 0 until width) {
            var r = 0; var g = 0; var b = 0; var count = 0
            val start = maxOf(0, x - radius)
            val end = minOf(width - 1, x + radius)
            for (i in start..end) {
                val p = pixels[yOffset + i]
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
                count++
            }
            outPixels[yOffset + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }

    for (x in 0 until width) {
        for (y in 0 until height) {
            var r = 0; var g = 0; var b = 0; var count = 0
            val start = maxOf(0, y - radius)
            val end = minOf(height - 1, y + radius)
            for (i in start..end) {
                val p = outPixels[i * width + x]
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
                count++
            }
            pixels[y * width + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
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
                            name = "美国运通 · 百夫长黑金卡",
                            type = AccountType.DEBIT,
                            initialBalance = 10000000L,
                            currentBalance = 11850000L,
                            cardNumber = "3759876543215288",
                            templateId = 2L,
                        ),
                        template = null,
                    ),
                    AccountWithTemplate(
                        account = Account(
                            id = 2L,
                            name = "微信支付",
                            type = AccountType.WALLET,
                            initialBalance = 200000L,
                            currentBalance = 1235000L,
                            cardNumber = "8820",
                            templateId = 3L,
                        ),
                        template = null,
                    ),
                    AccountWithTemplate(
                        account = Account(
                            id = 3L,
                            name = "招商银行 · 经典白金卡",
                            type = AccountType.CREDIT,
                            initialBalance = 0L,
                            currentBalance = 1400000L,
                            cardNumber = "4392258877619012",
                            templateId = 1L,
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

@Preview(showBackground = true)
@Composable
fun CompactBankCardListPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompactBankCardItem(
                account = Account(
                    id = 1L,
                    name = "美国运通 · 百夫长黑金卡",
                    type = AccountType.DEBIT,
                    initialBalance = 10000000L,
                    currentBalance = 11850000L,
                    cardNumber = "3759876543215288",
                    templateId = 2L,
                ),
                template = null,
                onAdjustClick = {},
            )
            CompactBankCardItem(
                account = Account(
                    id = 2L,
                    name = "微信支付",
                    type = AccountType.WALLET,
                    initialBalance = 200000L,
                    currentBalance = 1235000L,
                    cardNumber = "8820",
                    templateId = 3L,
                ),
                template = null,
                onAdjustClick = {},
            )
            CompactBankCardItem(
                account = Account(
                    id = 3L,
                    name = "招商银行 · 经典白金卡",
                    type = AccountType.CREDIT,
                    initialBalance = 0L,
                    currentBalance = 1400000L,
                    cardNumber = "4392258877619012",
                    templateId = 1L,
                ),
                template = null,
                onAdjustClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TwoCardsDeckPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            SwipeableBankCardDeck(
                accounts = listOf(
                    AccountWithTemplate(
                        account = Account(
                            id = 1L,
                            name = "美国运通 · 百夫长黑金卡",
                            type = AccountType.DEBIT,
                            initialBalance = 10000000L,
                            currentBalance = 11850000L,
                            cardNumber = "3759876543215288",
                            templateId = 2L,
                        ),
                        template = null,
                    ),
                    AccountWithTemplate(
                        account = Account(
                            id = 2L,
                            name = "微信支付",
                            type = AccountType.WALLET,
                            initialBalance = 200000L,
                            currentBalance = 1235000L,
                            cardNumber = "8820",
                            templateId = 3L,
                        ),
                        template = null,
                    ),
                ),
                onAdjustClick = {},
            )
        }
    }
}
