plugins {
    alias(libs.plugins.android.compose.convention)
}

android {
    namespace = "digital.tonima.kairos.core.ui"
}

// Shared design system (KairosTheme, Dimensions, Color, Type) and the handful of composables
// with zero cross-feature coupling (AdBannerView, parseMarkdownToAnnotatedString). Components
// that take a feature's UiState directly (e.g. PermissionGate today needs SettingsUiState)
// deliberately stay out of this module for now rather than pull :core in here backwards —
// see the modularization plan's Phase 2/6 notes.
dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.play.services.ads.api)
}
