package cn.turingzy.ledger.ui.component.card.summary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.ButtonBase
import cn.turingzy.ledger.ui.component.base.ButtonToneBase
import cn.turingzy.ledger.ui.theme.LedgerStyle

/** 在汇总卡片背面居中展示图片入口，同时保留横向滑动切回正面的手势。 */
@Composable
internal fun SummaryCardSettings(
    enabled: Boolean,
    onUploadBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ButtonBase(
            label = stringResource(R.string.ui_change_card_image),
            onClick = onUploadBackground,
            modifier = Modifier.heightIn(min = 50.dp),
            enabled = enabled,
            tone = ButtonToneBase.Neutral,
            trailingIcon = MaterialSymbols.AddPhotoAlternate,
            shape = LedgerStyle.shapes.pill,
        )
    }
}
