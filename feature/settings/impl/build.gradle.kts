plugins {
    id("android-compose-convention")
    id("android-feature-impl-convention")
}

android {
    namespace = "digital.tonima.kairos.feature.settings.impl"
}

dependencies {
    api(project(":core"))
    api(project(":core:data"))

    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.compose.icons.extended)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.logcat)
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":feature:settings:bridge"))

    testImplementation(libs.core.testing)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
