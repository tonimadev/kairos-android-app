plugins {
    alias(libs.plugins.android.library.convention)
}

android {
    namespace = "digital.tonima.kairos.core.navigation"
}

// No Hilt, no KSP, no Compose compiler here on purpose: this module is the one seam every
// feature bridge and every feature :impl is allowed to depend on unconditionally (see
// FeatureNavKey/AppNavigator kdoc), so it must stay contract-only.
dependencies {
    api(libs.androidx.navigation3.runtime)
}
