package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.SummaryCardIconButton
import cn.turingzy.ledger.ui.component.TopBarIconButton
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CompactIconButtonsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun summaryToolbarAndCloseShareGlyphAndCircularTargetDimensions() {
        compose.setContent { LedgerTheme { Column {
            SummaryCardIconButton(MaterialSymbols.MoreHoriz, "汇总操作", {})
            TopBarIconButton(MaterialSymbols.Search, "顶栏搜索", {})
            TopBarIconButton(MaterialSymbols.Close, "关闭搜索", {})
        } } }
        for (description in listOf("汇总操作", "顶栏搜索", "关闭搜索")) {
            val target = compose.onNodeWithContentDescription(description).getUnclippedBoundsInRoot()
            val glyph = compose.onNodeWithContentDescription(description, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertEquals(LedgerStyle.dimensions.compactIconButton, target.right - target.left)
            assertEquals(LedgerStyle.dimensions.compactIconButton, target.bottom - target.top)
            assertEquals(LedgerStyle.icons.default, glyph.right - glyph.left)
            assertEquals(LedgerStyle.icons.default, glyph.bottom - glyph.top)
        }
    }
}
