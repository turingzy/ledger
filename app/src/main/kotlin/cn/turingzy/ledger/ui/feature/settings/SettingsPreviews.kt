package cn.turingzy.ledger.ui.feature.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.ui.theme.LedgerTheme

internal class SettingsPreviewProvider : PreviewParameterProvider<SettingsUiState> {
    override val values: Sequence<SettingsUiState> = sequenceOf(
        SettingsUiState.Loading,
        SettingsUiState.Success(categories = emptyList()),
    )
}

@Preview(name = "Settings Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Settings Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreview(@PreviewParameter(SettingsPreviewProvider::class) state: SettingsUiState) {
    LedgerTheme { SettingsContent(state, {}, {}, {}, {}) }
}
