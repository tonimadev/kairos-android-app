plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.jacoco.convention)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.spotless)
}

android {
    namespace = "digital.tonima.kairos.core"
    compileSdk = rootProject.extra["COMPILE_SDK_VERSION"].toString().toInt()

    defaultConfig {
        minSdk = rootProject.extra["MIN_SDK_VERSION"].toString().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin { jvmToolchain(21) }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.wear)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.billing.ktx)
    implementation(libs.datastore.preferences.core)
    implementation(libs.google.firebase.analytics)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.inapp.update)
    implementation(libs.google.inapp.update.ktx)
    implementation(libs.hilt.android)
    implementation(libs.hilt.binder)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.worker)
    implementation(libs.logcat)
    implementation(platform(libs.google.firebase.bom))

    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.binder.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.core.testing)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
