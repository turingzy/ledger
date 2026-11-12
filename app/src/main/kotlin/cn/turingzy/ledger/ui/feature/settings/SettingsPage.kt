package cn.turingzy.ledger.ui.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.topBarPressFeedback
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.SurfaceLight

@Composable
internal fun SettingsPage(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    contentBottomPadding: Dp = 80.dp,
    avoidKeyboard: Boolean = false,
    action: @Composable () -> Unit = { Box(Modifier.size(48.dp)) },
    content: @Composable ColumnScope.() -> Unit,
) {
    val back = onBack ?: onDismissRequest
    BackHandler(onBack = back)
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        Modifier.weight(1f),
                        style = titleStyle,
                        fontWeight = FontWeight.Bold,
                    )
                    action()
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth()
                        .then(if (avoidKeyboard) Modifier.imePadding() else Modifier)
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = contentBottomPadding),
                    content = content,
                )
            }
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 16.dp)
                    .size(48.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(SurfaceLight)
                    .topBarPressFeedback(interactionSource, enabled = true, color = IosLabel)
                    .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = back),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MaterialSymbols.ArrowBack, "返回", tint = IosLabel)
            }
        }
    }
}
