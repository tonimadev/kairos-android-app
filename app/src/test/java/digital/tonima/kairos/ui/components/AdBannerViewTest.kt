package digital.tonima.kairos.ui.components

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.android.gms.ads.AdView
import digital.tonima.kairos.core.ads.components.AdBannerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xxhdpi")
class AdBannerViewTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the adaptive banner is sized to the screen width in dp, not in pixels`() {
        compose.setContent { AdBannerView(adId = TEST_AD_UNIT, isProUser = false, loadAd = false) }
        compose.waitForIdle()

        val adView = findAdView(compose.activity.window.decorView)

        assertEquals(400, adView?.adSize?.width)
    }

    @Test
    fun `pro users get no banner`() {
        compose.setContent { AdBannerView(adId = TEST_AD_UNIT, isProUser = true, loadAd = false) }
        compose.waitForIdle()

        assertNull(findAdView(compose.activity.window.decorView))
    }

    private fun findAdView(view: View): AdView? =
        when (view) {
            is AdView -> view
            is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findAdView(view.getChildAt(it)) }
            else -> null
        }

    private companion object {
        const val TEST_AD_UNIT = "ca-app-pub-3940256099942544/9214589741"
    }
}
