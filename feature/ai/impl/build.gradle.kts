plugins {
    alias(libs.plugins.kotlin.serialization)
    id("android-compose-convention")
    id("android-feature-impl-convention")
}

android {
    namespace = "digital.tonima.kairos.feature.ai.impl"

    ksp {
        arg("appfunctions:aggregateAppFunctions", "true")
    }
}

dependencies {
    api(project(":core"))
    api(project(":core:data"))

    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.compose.icons.extended)
    implementation(libs.datastore.preferences.core)
    implementation(libs.google.firebase.ia)
    implementation(libs.google.firebase.ia.ondevice)
    implementation(libs.guava)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.worker)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logcat)
    implementation(platform(libs.google.firebase.bom))
    implementation(project(":core:billing:bridge"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":feature:ai:bridge"))
    implementation(project(":feature:calendar:bridge"))
    implementation(project(":feature:settings:bridge"))

    ksp(libs.androidx.appfunctions.compiler)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.core.testing)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
