import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.jacoco.convention)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
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

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }

    val openWeatherApiKey = System.getenv("OPENWEATHER_API_KEY")
        ?: localProperties.getProperty("OPENWEATHER_API_KEY")
        ?: localProperties.getProperty("openweather.api.key")
        ?: ""

    val googleMapsApiKey = System.getenv("GOOGLE_MAPS_API_KEY")
        ?: localProperties.getProperty("GOOGLE_MAPS_API_KEY")
        ?: localProperties.getProperty("google.maps.api.key")
        ?: ""

    val webClientId = System.getenv("GOOGLE_WEB_CLIENT_ID")
        ?: localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
        ?: localProperties.getProperty("google.web.client.id")
        ?: ""

    buildTypes {
        debug {
            buildConfigField("String", "OPENWEATHER_API_KEY", "\"$openWeatherApiKey\"")
            buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$googleMapsApiKey\"")
            buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "OPENWEATHER_API_KEY", "\"$openWeatherApiKey\"")
            buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$googleMapsApiKey\"")
            buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
    }
    ksp {
        arg("appfunctions:aggregateAppFunctions", "true")
    }
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        checkDependencies = true
    }
    kotlin { jvmToolchain(21) }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    api(project(":core:billing:bridge"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.wear)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.datastore.preferences.core)
    implementation(libs.google.firebase.analytics)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.firebase.ia)
    implementation(libs.google.firebase.ia.ondevice)
    implementation(libs.google.inapp.update)
    implementation(libs.google.inapp.update.ktx)
    implementation(libs.hilt.android)
    implementation(libs.hilt.binder)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.worker)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.location.play.services)
    implementation(libs.logcat)
    implementation(libs.okhttp.logging)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.wearable)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(platform(libs.google.firebase.bom))
    implementation(project(":core:billing:impl"))

    ksp(libs.androidx.appfunctions.compiler)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.androidx.room.compiler)
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
