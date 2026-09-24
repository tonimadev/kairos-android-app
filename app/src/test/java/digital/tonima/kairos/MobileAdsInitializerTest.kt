package digital.tonima.kairos

import digital.tonima.core.analytics.CrashReporter
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MobileAdsInitializerTest {
    private val crashReporter: CrashReporter = mockk(relaxed = true)

    @Test
    fun `a device without WebView does not crash app startup and the failure is reported`() {
        val noWebView = UnsupportedOperationException()

        initializeMobileAds(crashReporter) { throw noWebView }

        verify { crashReporter.recordNonFatal(noWebView, any()) }
    }

    @Test
    fun `a successful initialization reports nothing`() {
        var initialized = false

        initializeMobileAds(crashReporter) { initialized = true }

        assert(initialized)
        verify(exactly = 0) { crashReporter.recordNonFatal(any(), any()) }
    }
}
