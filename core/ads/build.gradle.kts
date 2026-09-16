plugins {
    alias(libs.plugins.android.compose.convention)
}

android {
    namespace = "digital.tonima.kairos.core.ads"
}

// Isolates the AdMob SDK dependency (play-services-ads-api) so only modules that actually show
// ads pull in its manifest — notably com.google.android.gms.ads.MobileAdsInitProvider, which
// self-registers as a ContentProvider via manifest merge and crashes at process start on any app
// that lacks the com.google.android.gms.ads.APPLICATION_ID meta-data (e.g. :wear). This used to
// live in :core:ui, which :wear pulls in transitively through feature:calendar:impl and
// feature:settings:impl.
dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.play.services.ads.api)
}
