package cn.turingzy.ledger.ui

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.hilt.navigation.HiltViewModelFactory
import cn.turingzy.ledger.MainActivity
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class LocalizedActivityContextTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun localeOverridePreservesTheActivityRequiredByNavigationAndHilt() {
        compose.runOnUiThread {
            val activity = compose.activity
            listOf(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE).forEach { locale ->
                val configuration = Configuration(activity.resources.configuration).apply { setLocales(LocaleList(locale)) }
                val context = localizedActivityContext(activity, configuration)
                assertSame(activity, context.baseContext)
                assertEquals(locale.language, context.resources.configuration.locales[0].language)
                assertNotNull(HiltViewModelFactory(context, activity.defaultViewModelProviderFactory))
            }
        }
    }
}
