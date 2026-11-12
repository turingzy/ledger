package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.LedgerCardAppearance

@Composable
internal fun SummaryCardSettings(
    appearance: LedgerCardAppearance,
    enabled: Boolean,
    onUploadBackground: () -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onTransparencyChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val sliderEnabled = enabled
    Box(modifier) {
        Column(Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("透明度", Modifier.width(52.dp), fontSize = 13.sp, color = colors.onPrimary)
                BoxWithConstraints(Modifier.weight(1f).height(40.dp)) {
                    CompactSlider(
                        value = appearance.transparencyPercent.toFloat(),
                        onValueChange = onTransparencyChange,
                        onValueChangeFinished = onTransparencyChangeFinished,
                        enabled = sliderEnabled,
                        valueRange = 0f..100f,
                        color = colors.tertiary,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "透明度" },
                    )
                    Text(
                        "${appearance.transparencyPercent}%",
                        modifier = Modifier.offset(
                            x = (maxWidth - 40.dp).coerceAtLeast(0.dp) * (appearance.transparencyPercent / 100f),
                            y = (-2).dp,
                        ),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimary,
                    )
                }
            }
        }
        SummaryCardIconButton(
            imageVector = MaterialSymbols.AddPhotoAlternate,
            contentDescription = "更换卡片背景",
            onClick = onUploadBackground,
            enabled = enabled,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 12.dp).size(36.dp),
        )
    }
}
