package digital.tonima.kairos

import digital.tonima.core.analytics.CrashReporter

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
