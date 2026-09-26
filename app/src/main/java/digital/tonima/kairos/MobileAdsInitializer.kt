package digital.tonima.kairos

import digital.tonima.core.analytics.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * MobileAds reads the WebView user agent synchronously during initialization. Without a WebView
 * provider (Wear devices, or while the WebView package is being updated) that throws, and since
 * this runs from Application.onCreate it would crash the app on every launch.
 */
internal fun initializeMobileAds(
    crashReporter: CrashReporter,
    initialize: () -> Unit,
) {
    try {
        initialize()
    } catch (e: RuntimeException) {
        crashReporter.recordNonFatal(e, "MobileAds initialization failed; ads are disabled for this session")
    }
}

/**
 * MobileAds.initialize does disk and WebView work; Google recommends calling it off the main
 * thread so it does not slow down app startup or cause ANRs. Ads requested before it finishes
 * are queued by the SDK.
 */
internal fun CoroutineScope.launchMobileAdsInitialization(
    crashReporter: CrashReporter,
    initialize: () -> Unit,
): Job = launch { initializeMobileAds(crashReporter, initialize) }
