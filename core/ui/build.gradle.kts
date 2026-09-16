plugins {
    alias(libs.plugins.android.compose.convention)
}

android {
    namespace = "digital.tonima.kairos.core.ui"
}

// Shared design system (KairosTheme, Dimensions, Color, Type) and the handful of composables
// with zero cross-feature coupling (parseMarkdownToAnnotatedString). Components that take a
// feature's UiState directly (e.g. PermissionGate today needs SettingsUiState) deliberately stay
// out of this module for now rather than pull :core in here backwards — see the modularization
// plan's Phase 2/6 notes. AdBannerView lives in :core:ads instead, so modules that don't show
// ads (e.g. :wear, via feature:calendar:impl / feature:settings:impl) never pull in the AdMob
// SDK's manifest.
dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
