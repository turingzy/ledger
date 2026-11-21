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
