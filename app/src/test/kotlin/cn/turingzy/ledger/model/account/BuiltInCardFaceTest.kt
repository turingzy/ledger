package cn.turingzy.ledger.model.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInCardFaceTest {
    @Test
    fun oldTemplatesDoNotChangeWhichBuiltInFacesAreAvailable() {
        val legacy = CardTemplate(
            id = 2L,
            name = "商务黑金",
            coverUri = "res://drawable/card_black_gold",
            textColor = "#D4AF37",
            isSystem = true,
        )
        val builtIns = BuiltInCardFace.entries.map { it.asTemplate() }
        val deleted = legacy.copy(isDeleted = true)

        assertEquals(4, builtIns.map { it.id }.distinct().size)
        assertEquals(builtIns, (builtIns + legacy).selectableCardTemplates())
        assertEquals(builtIns, (builtIns + deleted).selectableCardTemplates())
        assertEquals(BuiltInCardFace.entries, builtIns.map(BuiltInCardFace::forTemplate))
        assertEquals(BuiltInCardFace.IridescentDark, BuiltInCardFace.forTemplate(legacy))
    }

    @Test
    fun customisedLegacyTemplatesRemainSelectable() {
        val legacy = CardTemplate(
            id = 2L,
            name = "商务黑金",
            coverUri = "res://drawable/card_black_gold",
            textColor = "#D4AF37",
            isSystem = true,
        )
        val modifications = listOf(
            legacy.copy(name = "我的银行卡"),
            legacy.copy(coverUri = "file:///card.png"),
            legacy.copy(textColor = "#FF220044"),
        )

        assertTrue(BuiltInCardFace.IridescentDark.isLegacyDefault(legacy))
        for (template in modifications) {
            assertFalse(BuiltInCardFace.IridescentDark.isLegacyDefault(template))
            assertEquals(listOf(template), listOf(template).selectableCardTemplates())
        }
    }

    @Test
    fun savingAppearanceKeepsCardIdentityAndReplacementImage() {
        val face = BuiltInCardFace.PearlLight
        val default = face.asTemplate()
        val appearance = default.copy(
            name = "我的珠光",
            textColor = "#FF223344",
            coverUri = "file:///different.png",
            isSystem = false,
        )

        val saved = face.asTemplate(appearance)
        assertEquals(appearance.copy(isSystem = true), saved)
        assertTrue(face.asTemplate(appearance.copy(isDeleted = true)).isDeleted)
        assertEquals(face, BuiltInCardFace.forTemplate(saved))
        assertTrue(face.hasDefaultName(default))
        assertFalse(face.hasDefaultName(saved))
    }

    @Test
    fun unassignedAccountsAndPristineLegacyFacesUseTheEditedBuiltInAppearance() {
        val default = BuiltInCardFace.IridescentLight.asTemplate().copy(textColor = "#FF665544")
        val twilight = BuiltInCardFace.IridescentDark.asTemplate().copy(name = "我的钛金")
        val legacy = CardTemplate(
            id = 2L,
            name = "商务黑金",
            coverUri = "res://drawable/card_black_gold",
            textColor = "#D4AF37",
            isSystem = true,
        )
        val templates = listOf(default, twilight)

        assertEquals(default, null.resolveBuiltInAppearance(templates))
        assertEquals(twilight, legacy.resolveBuiltInAppearance(templates))
        val custom = legacy.copy(textColor = "#FF665544")
        assertEquals(custom, custom.resolveBuiltInAppearance(templates))
    }
}
