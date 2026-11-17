package cn.turingzy.ledger.ui.feature.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.ui.theme.LedgerTheme

internal data class ParseRulesPreviewCase(val rules: List<ParseRulePreview>, val draft: ParseRulePreview? = null)

internal class ParseRulesPreviewProvider : PreviewParameterProvider<ParseRulesPreviewCase> {
    override val values: Sequence<ParseRulesPreviewCase> = sequence {
        val rules = defaultParseRulePreviews()
        yield(ParseRulesPreviewCase(rules))
        yield(ParseRulesPreviewCase(emptyList()))
        yield(ParseRulesPreviewCase(rules, rules.first()))
        yield(ParseRulesPreviewCase(rules, rules.first().copy(name = "")))
    }
}

@Preview(name = "Parse Rules Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Parse Rules Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ParseRulesPreview(@PreviewParameter(ParseRulesPreviewProvider::class) case: ParseRulesPreviewCase) {
    LedgerTheme { ParseRulesScreen(emptyList(), emptyList(), case.rules, {}, {}, {}, initialDraft = case.draft) }
}
