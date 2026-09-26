package digital.tonima.kairos

import digital.tonima.core.analytics.CrashReporter
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
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

    @Test
    fun `initialization does not run on the calling thread but on the given dispatcher`() {
        val dispatcher = StandardTestDispatcher()
        var initialized = false

        CoroutineScope(dispatcher).launchMobileAdsInitialization(crashReporter) { initialized = true }

        assert(!initialized)
        dispatcher.scheduler.advanceUntilIdle()
        assert(initialized)
    }

    @Test
    fun `a failure during background initialization is reported instead of crashing`() {
        val dispatcher = StandardTestDispatcher()
        val noWebView = UnsupportedOperationException()

        CoroutineScope(dispatcher).launchMobileAdsInitialization(crashReporter) { throw noWebView }
        dispatcher.scheduler.advanceUntilIdle()

        verify { crashReporter.recordNonFatal(noWebView, any()) }
    }
}
