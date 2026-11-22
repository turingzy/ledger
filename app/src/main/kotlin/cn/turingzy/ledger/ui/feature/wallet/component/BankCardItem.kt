package cn.turingzy.ledger.ui.feature.wallet.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.component.card.bank.TitaniumBankCard
import cn.turingzy.ledger.ui.component.card.bank.TitaniumDrag
import cn.turingzy.ledger.ui.component.card.bank.titaniumImageUri
import cn.turingzy.ledger.ui.component.card.bank.titaniumInkOverride
import cn.turingzy.ledger.ui.component.card.bank.titaniumPreset

/**
 * 钱包账户银行卡片渲染项组件。
 *
 * 将账户模型与卡面模板映射为钛金属质感银行卡（[TitaniumBankCard]），支持卡号尾号展示、长按拖拽与点击交互。
 *
 * @param account 对应的账户对象
 * @param template 关联的银行卡卡面模板（可为空）
 * @param modifier 外部修饰符
 * @param motionEnabled 是否开启陀螺仪/手势动态光影效果
 * @param onAdjustClick 点击卡片回调（通常用于查看详情或调整余额）
 */
@Composable
internal fun BankCardItem(
    account: Account,
    template: CardTemplate?,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
    onAdjustClick: (() -> Unit)? = null,
) {
    val isBankAccount = account.type == AccountType.DEBIT || account.type == AccountType.CREDIT
    TitaniumBankCard(
        name = account.name,
        modifier = modifier,
        preset = template.titaniumPreset(),
        tailNumber = account.cardNumber.takeIf { isBankAccount },
        imageUri = template.titaniumImageUri(),
        textColor = template.titaniumInkOverride(),
        motionEnabled = motionEnabled,
        drag = TitaniumDrag.AfterLongPress,
        onClick = onAdjustClick,
    )
}
