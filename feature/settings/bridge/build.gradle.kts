plugins {
    id("android-feature-bridge-convention")
}

android {
    namespace = "digital.tonima.kairos.feature.settings.bridge"
}

// SettingsIntent lives here (not in :impl) for the same reason as EventIntent in
// feature:calendar:bridge — feature:ai:impl needs it as a plain data vocabulary only.
dependencies {
    api(project(":core"))
    api(project(":core:navigation"))
}
