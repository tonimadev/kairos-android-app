package digital.tonima.kairos.core.ads.consent

import android.app.Activity
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsConsentManagerTest {
    private class FakeSource(
        var canRequestAds: Boolean = false,
        var privacyOptionsRequired: Boolean = false,
    ) : ConsentSource {
        var gatherGrantsAds = true
        var gatherError: String? = null
        var gatherCalls = 0

        override fun canRequestAds() = canRequestAds

        override fun isPrivacyOptionsRequired() = privacyOptionsRequired

        override fun gather(
            activity: Activity,
            onDone: (errorMessage: String?) -> Unit,
        ) {
            gatherCalls++
            if (gatherError == null) canRequestAds = gatherGrantsAds
            onDone(gatherError)
        }

        override fun showPrivacyOptions(
            activity: Activity,
            onDone: (errorMessage: String?) -> Unit,
        ) {
            privacyOptionsRequired = true
            canRequestAds = !canRequestAds
            onDone(null)
        }
    }

    private val activity: Activity = mockk(relaxed = true)

    @Test
    fun `ads stay blocked until consent is gathered`() {
        val manager = AdsConsentManager(FakeSource(canRequestAds = false))

        assertFalse(manager.canRequestAds.value)

        manager.gather(activity)

        assertTrue(manager.canRequestAds.value)
    }

    @Test
    fun `a returning user who already consented can request ads before the form round trip`() {
        val manager = AdsConsentManager(FakeSource(canRequestAds = true))

        assertTrue(manager.canRequestAds.value)
    }

    @Test
    fun `a user who declined stays blocked after gathering`() {
        val source = FakeSource().apply { gatherGrantsAds = false }
        val manager = AdsConsentManager(source)

        manager.gather(activity)

        assertFalse(manager.canRequestAds.value)
    }

    @Test
    fun `a failed consent request keeps the cached decision instead of crashing`() {
        val granted = AdsConsentManager(FakeSource(canRequestAds = true).apply { gatherError = "offline" })
        val notGranted = AdsConsentManager(FakeSource(canRequestAds = false).apply { gatherError = "offline" })

        granted.gather(activity)
        notGranted.gather(activity)

        assertTrue(granted.canRequestAds.value)
        assertFalse(notGranted.canRequestAds.value)
    }

    @Test
    fun `changing the choice from the privacy options form is published`() {
        val source = FakeSource(canRequestAds = true)
        val manager = AdsConsentManager(source)

        manager.showPrivacyOptions(activity)

        assertFalse(manager.canRequestAds.value)
        assertTrue(manager.isPrivacyOptionsRequired.value)
    }

    @Test
    fun `before install nothing may request ads`() {
        assertFalse(AdsConsent.canRequestAds.value)
        assertFalse(AdsConsent.isPrivacyOptionsRequired.value)
    }
}
