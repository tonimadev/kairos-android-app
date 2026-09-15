plugins {
    id("android-feature-impl-convention")
}

android {
    namespace = "digital.tonima.kairos.core.data"
}

// Shared domain/data layer: repositories + usecases consumed by 2+ features (or by :app-shell
// code outside any feature — receivers/workers/widget), plus the usecases whose repository is
// itself here. Feature-exclusive repositories/usecases (e.g. chat history, which is AI-only)
// live inside that feature's own :impl instead — see the modularization plan's file buckets.
dependencies {
    api(project(":core"))
    api(project(":core:model"))

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coroutines.play.services)
    implementation(libs.guava)
    implementation(libs.hilt.worker)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.location.play.services)
    implementation(libs.logcat)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)

    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
