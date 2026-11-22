package cn.turingzy.ledger.ui.component.biz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 账户徽标图标组件。
 *
 * 根据账户类型（储蓄卡、信用卡、钱包、现金）及名称特征（如支付宝、微信）呈现对应的 Material Symbols 图标与专属底色。
 *
 * @param account 待展示的账户对象。
 * @param modifier 应用于图标容器的修饰符。
 */
@Composable
fun AccountIconBiz(account: Account, modifier: Modifier = Modifier) {
    val colors = LedgerStyle.colors
    val background = when (account.displayAccent()) {
        AccountAccent.Primary -> colors.accountDebit
        AccountAccent.Income -> colors.accountWallet
        AccountAccent.Credit -> colors.accountCredit
        AccountAccent.Cash -> colors.accountCash
    }
    val icon = when (account.type) {
        AccountType.DEBIT -> MaterialSymbols.AccountBalance
        AccountType.CREDIT -> MaterialSymbols.Contactless
        AccountType.WALLET -> MaterialSymbols.AccountBalanceWallet
        AccountType.CASH -> MaterialSymbols.Payments
    }
    Box(
        modifier.size(LedgerStyle.dimensions.compactIconButton).clip(LedgerStyle.shapes.pill).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(LedgerStyle.icons.inline), tint = MaterialTheme.colorScheme.onPrimary)
    }
}

/**
 * 账户视觉强调色分类。
 */
internal enum class AccountAccent {
    /** 主要/借记卡色调（如蓝色）。 */
    Primary,
    /** 电子钱包/绿色调（如微信钱包）。 */
    Income,
    /** 信用卡色调（如橙色/紫色）。 */
    Credit,
    /** 现金色调（如黄色/棕色）。 */
    Cash,
}

/**
 * 根据账户名称关键词和账户类型计算对应的视觉色调分类。
 *
 * @return 对应的 [AccountAccent] 强调色枚举。
 */
internal fun Account.displayAccent(): AccountAccent = when {
    name.contains("支付宝") -> AccountAccent.Primary
    name.contains("微信") -> AccountAccent.Income
    type == AccountType.DEBIT -> AccountAccent.Primary
    type == AccountType.CREDIT -> AccountAccent.Credit
    type == AccountType.CASH -> AccountAccent.Cash
    else -> AccountAccent.Income
}

/**
 * 交易类型圆形图标组件。
 *
 * 在紧凑的药丸圆角背景中展示指定矢量图标，支持按交易类型（支出、收入、转账）高亮着色或弱化显示。
 *
 * @param imageVector 图标矢量图。
 * @param type 交易类型（支出、收入、转账）。
 * @param modifier 应用于容器的修饰符。
 * @param contentDescription 无障碍文本描述。
 * @param highlighted 是否高亮显示；为 true 时采用交易类型主题色底色与前景色，为 false 时采用次要浅灰色调。
 */
@Composable
internal fun TypeIconBiz(
    imageVector: ImageVector,
    type: TransactionType,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    highlighted: Boolean = true,
) {
    val colors = LedgerStyle.colors
    Box(
        modifier.size(LedgerStyle.dimensions.compactIconButton).clip(LedgerStyle.shapes.pill)
            .background(if (highlighted) colors.transaction(type) else colors.fieldBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(LedgerStyle.icons.inline),
            tint = if (highlighted) colors.onTransaction(type) else colors.textSecondary,
        )
    }
}
