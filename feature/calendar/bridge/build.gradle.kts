plugins {
    id("android-feature-bridge-convention")
}

android {
    namespace = "digital.tonima.kairos.feature.calendar.bridge"
}

// EventIntent lives here (not in :impl) because feature:ai:impl needs it purely as a data
// vocabulary — an AI tool builds an EventIntent and AiViewModel re-executes it with its own
// usecases, never touching EventViewModel or any calendar UI. See CreateEventTool etc.
dependencies {
    api(project(":core"))
    api(project(":core:navigation"))
}
