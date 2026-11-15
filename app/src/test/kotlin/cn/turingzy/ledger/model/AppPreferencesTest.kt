package cn.turingzy.ledger.model

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class AppPreferencesTest {
    @Test fun primaryCurrencyUsesThePhoneRegionRatherThanItsLanguage() {
        assertEquals("CNY", defaultPrimaryCurrency(Locale.CHINA))
        assertEquals("USD", defaultPrimaryCurrency(Locale.US))
        assertEquals("GBP", defaultPrimaryCurrency(Locale.UK))
        assertEquals("JPY", defaultPrimaryCurrency(Locale.JAPAN))
        assertEquals("EUR", defaultPrimaryCurrency(Locale.GERMANY))
        assertEquals("CNY", defaultPrimaryCurrency(Locale.forLanguageTag("en-CN")))
        assertEquals("USD", defaultPrimaryCurrency(Locale.forLanguageTag("zh-US")))
    }

    @Test fun regionlessLocalesHaveAUsableFallback() {
        assertEquals("USD", defaultPrimaryCurrency(Locale.ROOT))
        assertEquals("USD", defaultPrimaryCurrency(Locale.ENGLISH))
        assertTrue(defaultPrimaryCurrency(Locale.forLanguageTag("en-AQ")) in supportedCurrencyCodes)
    }
}
