import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.jacoco.convention)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.spotless)
}

val isRunningReleaseTask: Boolean = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

android {
    namespace = "digital.tonima.kairos"
    compileSdk = rootProject.extra["COMPILE_SDK_VERSION"].toString().toInt()

    defaultConfig {
        applicationId = "digital.tonima.kairos"
        minSdk = rootProject.extra["MIN_SDK_VERSION"].toString().toInt()
        targetSdk = rootProject.extra["TARGET_SDK_VERSION"].toString().toInt()
        versionCode = findProperty("android.injected.version.code")?.toString()?.toInt()
            ?: rootProject.extra["APP_VERSION_CODE"].toString().toInt()
        versionName = findProperty("android.injected.version.name")?.toString()
            ?: rootProject.extra["APP_VERSION_NAME"].toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks")
            storePassword = System.getenv("ANDROID_SIGNING_KEY_PASSWORD") ?: "default_store_password_app"
            keyAlias = System.getenv("ANDROID_SIGNING_KEY_ALIAS") ?: "default_key_alias_app"
            keyPassword = System.getenv("ANDROID_SIGNING_KEY_ALIAS_PASSWORD") ?: "default_key_password_app"
        }
    }

    buildTypes {
        debug {
            val admobAppIdTest = "ca-app-pub-3940256099942544~3347511713"
            val admobBannerAdUnitIdTest = "ca-app-pub-3940256099942544/6300978111"

            resValue("string", "admob_app_id", admobAppIdTest)
            buildConfigField("String", "ADMOB_BANNER_AD_UNIT_HOME", "\"$admobBannerAdUnitIdTest\"")
            buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY", "\"$admobBannerAdUnitIdTest\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfig = signingConfigs.getByName("release")

            val admobAppIdTest = "ca-app-pub-3940256099942544~3347511713"
            val admobBannerAdUnitIdTest = "ca-app-pub-3940256099942544/9214589741"

            val admobAppId: String
            val admobBannerAdUnitIdHome: String
            val admobBannerAdUnitIdAlarm: String

            if (isRunningReleaseTask) {
                val localProperties = Properties()
                val localPropertiesFile = rootProject.file("local.properties")
                if (localPropertiesFile.exists()) {
                    localProperties.load(FileInputStream(localPropertiesFile))
                }

                admobAppId =
                    System.getenv("ADMOB_APP_ID")
                        ?: localProperties.getProperty("admob.app.id")
                        ?: admobAppIdTest

                admobBannerAdUnitIdHome =
                    System.getenv("ADMOB_BANNER_AD_UNIT_HOME")
                        ?: localProperties.getProperty("admob.banner.ad.unit.home")
                        ?: admobBannerAdUnitIdTest
                admobBannerAdUnitIdAlarm =
                    System.getenv("ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY")
                        ?: localProperties.getProperty("admob.banner.ad.unit.alarm_acitivity")
                        ?: admobBannerAdUnitIdTest
            } else {
                admobAppId = admobAppIdTest
                admobBannerAdUnitIdHome = admobBannerAdUnitIdTest
                admobBannerAdUnitIdAlarm = admobBannerAdUnitIdTest
            }

            resValue("string", "admob_app_id", admobAppId)
            buildConfigField("String", "ADMOB_BANNER_AD_UNIT_HOME", "\"$admobBannerAdUnitIdHome\"")
            buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY", "\"$admobBannerAdUnitIdAlarm\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.calendar)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size.class1)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.google.firebase.analytics)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.inapp.update)
    implementation(libs.google.inapp.update.ktx)
    implementation(libs.hilt.android)
    implementation(libs.hilt.binder)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.worker)
    implementation(libs.logcat)
    implementation(libs.play.services.ads.api)
    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.google.firebase.bom))
    implementation(project(":core"))

    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.binder.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

apply(from = "../spotless.gradle")
