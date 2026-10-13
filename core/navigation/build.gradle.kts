plugins {
    alias(libs.plugins.kmp.library.convention)
}

// No Hilt, no KSP, no Compose compiler here on purpose: this module is the one seam every
// feature bridge and every feature :impl is allowed to depend on unconditionally (see
// FeatureNavKey/AppNavigator kdoc), so it must stay contract-only.
kotlin {
    sourceSets {
        commonMain {
            // Sources stay in the legacy layout so the move to KMP is a build-only change.
            kotlin.srcDir("src/main/java")
            dependencies {
                api(libs.androidx.navigation3.runtime)
            }
        }
    }
}
