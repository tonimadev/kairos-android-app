plugins {
    id("android-feature-bridge-convention")
}

android {
    namespace = "digital.tonima.kairos.feature.ai.bridge"
}

dependencies {
    api(project(":core:navigation"))
}
