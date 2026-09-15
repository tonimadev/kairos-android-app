plugins {
    id("android-feature-impl-convention")
}

android {
    namespace = "digital.tonima.kairos.feature.alarm.impl"
}

// AlarmViewModel/Intent/UiState/SideEffect only — plain MVI classes shared by the phone's
// AlarmActivity (in :app) and :wear's WearAlarmActivity. AlarmActivity itself stays in :app: it
// is phone-only, and having it here put it on :wear's classpath too, which made Hilt try (and
// fail) to satisfy its phone-only bindings (AdMob unit id) for the wear Hilt graph as well.
dependencies {
    api(project(":core"))
    api(project(":core:data"))

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.logcat)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}
