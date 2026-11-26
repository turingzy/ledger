package cn.turingzy.ledger.ui.component.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.IconButtonBase
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 顶级页面共用的记账入口，齿轮底色与交易卡片一致。
 */
@Composable
internal fun LedgerFloatingActionButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier,
    ) {
        IconButtonBase(
            imageVector = MaterialSymbols.Add,
            contentDescription = stringResource(R.string.ui_record_transaction),
            onClick = onClick,
            modifier = Modifier
                .dropShadow(
                    shape = RecordButtonShape,
                    shadow = Shadow(
                        radius = LedgerStyle.dimensions.floatingActionShadow,
                        color = MaterialTheme.colorScheme.scrim,
                        offset = DpOffset(0.dp, 2.dp),
                        alpha = 0.22f,
                    ),
                )
                .recordButtonBackground(LedgerStyle.colors.surface),
            enabled = visible,
            contentColor = LedgerStyle.colors.textPrimary,
            iconSize = 28.dp,
            interaction = UiInteractionKind.Navigation,
        )
    }
}

/** 绘制底板，并将图标和按压高亮裁剪在同一条波浪轮廓内。 */
private fun Modifier.recordButtonBackground(color: Color): Modifier = drawWithCache {
    val outline = RecordButtonShape.createOutline(size, layoutDirection, this).path
    onDrawWithContent {
        drawPath(outline, color)
        clipPath(outline) { this@onDrawWithContent.drawContent() }
    }
}

/** 阴影和底板共用波峰、波谷以切线相连的轮廓。 */
private object RecordButtonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline.Generic {
        val lobeCount = 9
        val angleStep = PI.toFloat() / lobeCount
        val tangentScale = angleStep / 3f
        val outerRadius = size.minDimension / 2f - with(density) { 1.dp.toPx() }
        val center = size.center
        val points = List(lobeCount * 2) { index ->
            val angle = index * angleStep
            val radius = if (index % 2 == 0) outerRadius else outerRadius * 0.92f
            Offset(sin(angle), -cos(angle)) * radius
        }

        val outline = Path().apply {
            val start = center + points.first()
            moveTo(start.x, start.y)
            points.forEachIndexed { index, from ->
                val to = points[(index + 1) % points.size]
                val firstControl = center + from + Offset(-from.y, from.x) * tangentScale
                val secondControl = center + to - Offset(-to.y, to.x) * tangentScale
                val end = center + to
                cubicTo(firstControl.x, firstControl.y, secondControl.x, secondControl.y, end.x, end.y)
            }
            close()
        }
        return Outline.Generic(outline)
    }
}
