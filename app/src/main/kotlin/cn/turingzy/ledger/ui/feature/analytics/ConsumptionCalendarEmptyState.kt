package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols

@Composable
internal fun ConsumptionCalendarEmptyState(
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = colors.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painterResource(R.drawable.illustration_calendar_empty),
                contentDescription = null,
                modifier = Modifier.size(width = 140.dp, height = 105.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.ui_no_expenses_on_this_day), fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Text(stringResource(R.string.calendar_empty_supporting), Modifier.padding(top = 8.dp),
                fontSize = 13.sp, lineHeight = 20.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Surface(onClick = onRecord, shape = CircleShape, color = colors.background) {
                Row(Modifier.height(36.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(stringResource(R.string.ui_new_transaction), fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, color = colors.onSurfaceVariant)
                    Icon(MaterialSymbols.EditSquare, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}
