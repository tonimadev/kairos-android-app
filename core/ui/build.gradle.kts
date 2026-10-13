plugins {
    alias(libs.plugins.kmp.compose.convention)
}

// Shared design system (KairosTheme, Dimensions, Color, Type) and the handful of composables
// with zero cross-feature coupling (parseMarkdownToAnnotatedString). Components that take a
// feature's UiState directly (e.g. PermissionGate today needs SettingsUiState) deliberately stay
// out of this module for now rather than pull :core in here backwards — see the modularization
// plan's Phase 2/6 notes. AdBannerView lives in :core:ads instead, so modules that don't show
// ads (e.g. :wear, via feature:calendar:impl / feature:settings:impl) never pull in the AdMob
// SDK's manifest.
kotlin {
    androidLibrary {
        namespace = "digital.tonima.kairos.core.ui"
    }

    sourceSets {
        commonMain {
            // Sources stay in the legacy layout so the move to KMP is a build-only change.
            kotlin.srcDir("src/main/java")
        }
    }
}
