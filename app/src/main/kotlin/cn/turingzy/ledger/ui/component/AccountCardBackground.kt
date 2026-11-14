package cn.turingzy.ledger.ui.component

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate

@Composable
internal fun AccountCardBackground(account: Account, template: CardTemplate?, modifier: Modifier = Modifier) {
    val activeTemplate = template?.takeUnless { it.isDeleted }
    val styledAccount = if (template?.isDeleted == true) account.copy(templateId = null) else account
    val cover = activeTemplate?.coverUri?.let(Uri::parse)
    Box(modifier.background(Brush.linearGradient(accountCardColors(styledAccount)))) {
        if (cover?.scheme in setOf("content", "file", "android.resource")) {
            AndroidView(
                factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                update = { image ->
                    if (image.tag != cover) { image.setImageURI(cover); image.tag = cover }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun accountCardColors(account: Account): List<Color> {
    val isCenturion = account.templateId == 2L || account.name.contains("百夫长") || account.name.contains("运通")
    return when {
        isCenturion -> listOf(Color(0xFF0D0E11), Color(0xFF141519), Color(0xFF08080A))
        account.templateId == 1L -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)) // 经典深蓝
        account.templateId == 3L -> listOf(Color(0xFF075E37), Color(0xFF03361E)) // 翡翠绿 (微信/活力薄荷)
        account.type == AccountType.CREDIT -> listOf(Color(0xFF2D143D), Color(0xFF1A0D26)) // 尊贵紫 (白金信用卡)
        account.type == AccountType.WALLET && account.name.contains("支付宝") -> listOf(Color(0xFF0A438C), Color(0xFF052148)) // 皇家科技蓝
        else -> listOf(Color(0xFF1C1C1E), Color(0xFF121214)) // 黑曜金 (金葵花/储蓄卡)
    }
}

