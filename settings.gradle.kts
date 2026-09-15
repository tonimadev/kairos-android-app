pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
includeBuild("build-logic")

rootProject.name = "Kairos"
include(":app")
include(":core")
include(":core:data")
include(":core:model")
include(":core:navigation")
include(":core:ui")
include(":core:billing:bridge")
include(":core:billing:impl")
include(":feature:calendar:bridge")
include(":feature:calendar:impl")
include(":feature:settings:bridge")
include(":feature:settings:impl")
include(":feature:ai:bridge")
include(":feature:ai:impl")
include(":feature:alarm:impl")
include(":wear")
