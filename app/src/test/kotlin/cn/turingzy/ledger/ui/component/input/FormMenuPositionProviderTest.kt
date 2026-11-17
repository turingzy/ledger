package cn.turingzy.ledger.ui.component.input

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class FormMenuPositionProviderTest {
    private val field = IntRect(20, 300, 320, 348)
    private val window = IntSize(360, 800)
    private val menu = IntSize(160, 116)

    @Test fun generalSettingsOpenBelowTheFieldAlignedToItsTrailingArrow() {
        val position = FormMenuPositionProvider(false, DropdownDirection.DOWN)
            .calculatePosition(field, window, LayoutDirection.Ltr, menu)
        assertEquals(IntOffset(160, 348), position)
        assertEquals(field.right, position.x + menu.width)
    }

    @Test fun accountFormsKeepTheirExistingUpwardDirection() {
        val position = FormMenuPositionProvider(false)
            .calculatePosition(field, window, LayoutDirection.Ltr, menu)
        assertEquals(IntOffset(160, 184), position)
        assertEquals(field.top, position.y + menu.height)
    }

    @Test fun downwardMenusRemainBelowTheirAnchorNearTheBottomOfTheWindow() {
        val field = IntRect(20, 710, 320, 758)
        val availableMenu = IntSize(160, 42)
        val position = FormMenuPositionProvider(false, DropdownDirection.DOWN)
            .calculatePosition(field, window, LayoutDirection.Ltr, availableMenu)
        assertEquals(field.bottom, position.y)
        assertEquals(window.height, position.y + availableMenu.height)
    }
}
