import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.detekt)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.jacoco.convention)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.stability.analyzer)
}

val isRunningReleaseTask: Boolean = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

// AdMob ids that were not configured for a release build; see verifyReleaseAdMobConfig below.
val missingReleaseAdMobConfig = mutableListOf<String>()

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

                // Unset GitHub secrets expand to an empty string rather than a missing variable, so
                // blank values must fall through too.
                fun admobValue(
                    name: String,
                    vararg localKeys: String,
                ): String? =
                    (
                        listOf(findProperty(name)?.toString(), System.getenv(name)) +
                            localKeys.map { localProperties.getProperty(it) }
                    ).firstOrNull { !it.isNullOrBlank() }

                val resolvedAppId = admobValue("ADMOB_APP_ID", "admob.app.id")
                val resolvedHome = admobValue("ADMOB_BANNER_AD_UNIT_HOME", "admob.banner.ad.unit.home")
                val resolvedAlarm =
                    admobValue(
                        "ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY",
                        "admob.banner.ad.unit.alarm_activity",
                        "admob.banner.ad.unit.alarm_acitivity",
                    )
                missingReleaseAdMobConfig +=
                    listOf(
                        "ADMOB_APP_ID" to resolvedAppId,
                        "ADMOB_BANNER_AD_UNIT_HOME" to resolvedHome,
                        "ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY" to resolvedAlarm,
                    ).filter { (_, value) -> value == null }.map { it.first }

                admobAppId = resolvedAppId ?: admobAppIdTest
                admobBannerAdUnitIdHome = resolvedHome ?: admobBannerAdUnitIdTest
                admobBannerAdUnitIdAlarm = resolvedAlarm ?: admobBannerAdUnitIdTest
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

    ksp {
        arg("appfunctions:aggregateAppFunctions", "true")
    }
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        checkDependencies = true
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
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.compose.calendar)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size.class1)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.compose.icons.extended)
    implementation(libs.google.firebase.analytics)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.firebase.perf)
    implementation(libs.google.inapp.update)
    implementation(libs.google.inapp.update.ktx)
    implementation(libs.guava)
    implementation(libs.hilt.android)
    implementation(libs.hilt.binder)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.worker)
    implementation(libs.logcat)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)
    implementation(libs.play.services.ads.api)
    implementation(libs.play.services.wearable)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.google.firebase.bom))
    implementation(project(":core"))
    implementation(project(":core:ads"))
    implementation(project(":core:billing:bridge"))
    implementation(project(":core:billing:impl"))
    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":feature:ai:bridge"))
    implementation(project(":feature:ai:impl"))
    implementation(project(":feature:alarm:impl"))
    implementation(project(":feature:calendar:bridge"))
    implementation(project(":feature:calendar:impl"))
    implementation(project(":feature:settings:bridge"))
    implementation(project(":feature:settings:impl"))

    ksp(libs.androidx.appfunctions.compiler)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.binder.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}

// Robolectric 4.17 reflects into JDK internals (jdk.internal.access.SharedSecrets and friends)
// to bootstrap ApplicationSharedMemory; on JDK 17+ that throws IllegalAccessException unless
// these packages are explicitly opened to the test JVM. See
// https://github.com/robolectric/robolectric/releases/tag/robolectric-4.17.
tasks.withType<Test>().configureEach {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    )
}

// A release built without the real AdMob ids silently ships Google's test ids, and the published
// app earns nothing. Fail the packaging of the phone release instead. The check runs at execution
// time, not while configuring, so :wear:bundleRelease (which also configures this project) is not
// affected. Pass -Pkairos.allowTestAdMobIds=true to build a local release with test ads.
val verifyReleaseAdMobConfig by tasks.registering {
    val missing = missingReleaseAdMobConfig.joinToString()
    val allowTestIds = findProperty("kairos.allowTestAdMobIds")?.toString().toBoolean()
    doLast {
        if (missing.isNotEmpty() && !allowTestIds) {
            throw GradleException(
                "Release build is missing AdMob config ($missing); it would ship test ads. " +
                    "Set them as Gradle properties, env vars or in local.properties, " +
                    "or pass -Pkairos.allowTestAdMobIds=true for a local test build.",
            )
        }
    }
}
tasks.matching { it.name == "packageRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseAdMobConfig)
}

apply(from = "../spotless.gradle")
