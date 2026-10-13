plugins {
    alias(libs.plugins.kmp.library.convention)
    alias(libs.plugins.kotlin.serialization)
}

// Shared domain module (JVM for :app/:wear, iOS for the port). No `com.android.*` plugin is applied
// anywhere in this file, on purpose: that is what stops an Android or Compose import from ever
// compiling in here again, rather than relying on convention alone.
kotlin {
    sourceSets {
        commonMain {
            // Sources stay in the legacy layout so the move to KMP is a build-only change.
            kotlin.srcDir("src/main/java")
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
        jvmTest {
            kotlin.srcDir("src/test/java")
            dependencies {
                implementation(libs.junit)
            }
        }
    }
}
