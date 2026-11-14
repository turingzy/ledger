package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.theme.IosYellow
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
internal fun TransactionRow(
    categoryName: String,
    supportingText: String,
    timeText: String,
    amount: Long,
    type: TransactionType,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val typeColor = when (type) {
        TransactionType.EXPENSE -> colors.error
        TransactionType.INCOME -> colors.tertiary
        TransactionType.TRANSFER -> IosYellow
    }
    val amountText = when (type) {
        TransactionType.EXPENSE -> "−${amount.toFormattedAmount(showSymbol = true)}"
        TransactionType.INCOME -> "+${amount.toFormattedAmount(showSymbol = true)}"
        TransactionType.TRANSFER -> amount.toFormattedAmount(showSymbol = true)
    }
    Row(
        modifier = modifier.fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            ))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(typeColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = categoryName,
                tint = if (type == TransactionType.TRANSFER) colors.scrim else colors.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = categoryName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = colors.onSurface,
                letterSpacing = (-0.2).sp,
            )
            Text(
                text = supportingText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colors.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = amountText,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = if (type == TransactionType.TRANSFER) colors.onSurface else typeColor,
                letterSpacing = (-0.3).sp,
            )
            Text(
                text = timeText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
