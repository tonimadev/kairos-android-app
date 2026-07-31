// Top-level build file where you can add configuration options common to all sub-projects/modules.
val appVersionCode = project.findProperty("APP_VERSION_CODE") ?: "1"
val wearAppVersionCode = project.findProperty("WEAR_APP_VERSION_CODE") ?: "1"
val appVersionName = project.findProperty("APP_VERSION_NAME") ?: "1.0.0"

val minSdkVersion = project.findProperty("MIN_SDK_VERSION")?.toString()?.toInt() ?: 30
val targetSdkVersion = project.findProperty("TARGET_SDK_VERSION")?.toString()?.toInt() ?: 37
val compileSdkVersion = project.findProperty("COMPILE_SDK_VERSION")?.toString()?.toInt() ?: 37

extra.set("APP_VERSION_CODE", appVersionCode)
extra.set("WEAR_APP_VERSION_CODE", wearAppVersionCode)
extra.set("APP_VERSION_NAME", appVersionName)
extra.set("MIN_SDK_VERSION", minSdkVersion)
extra.set("TARGET_SDK_VERSION", targetSdkVersion)
extra.set("COMPILE_SDK_VERSION", compileSdkVersion)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.jacoco.convention) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.spotless)
    id("jacoco")
}

apply(from = "spotless.gradle")

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
}

val sortDependencies by tasks.registering {
    group = "Verification"
    description = "Checks and sorts dependencies and plugins in build.gradle.kts files with spacing between groups."

    val buildFilesFromConfig = project.allprojects.map { it.file("build.gradle.kts") }.filter { it.exists() }
    inputs.files(buildFilesFromConfig)

    doLast {
        inputs.files.forEach { file ->
            val lines = file.readLines()
            val newLines = mutableListOf<String>()
            var i = 0
            while (i < lines.size) {
                val line = lines[i]
                if (line.trim().startsWith("plugins {") || line.trim().startsWith("dependencies {")) {
                    val isDependencies = line.trim().startsWith("dependencies {")
                    newLines.add(line)
                    val blockLines = mutableListOf<String>()
                    i++
                    var openBraces = 1
                    while (i < lines.size && openBraces > 0) {
                        val currentLine = lines[i]
                        openBraces += currentLine.count { it == '{' }
                        openBraces -= currentLine.count { it == '}' }
                        if (openBraces > 0) {
                            blockLines.add(currentLine)
                            i++
                        }
                    }

                    if (isDependencies) {
                        val groups = mutableMapOf<String, MutableList<String>>()
                        val other = mutableListOf<String>()

                        var currentComments = mutableListOf<String>()

                        blockLines.forEach { bl ->
                            val trimmed = bl.trim()
                            if (trimmed.isEmpty()) return@forEach

                            if (trimmed.startsWith("//")) {
                                currentComments.add(bl)
                            } else {
                                val match = Regex("^([a-zA-Z]+)\\(.*\\)$").find(trimmed)
                                val groupName = match?.groupValues?.get(1) ?: "other"

                                val entry = (currentComments + bl).joinToString("\n")
                                if (groupName != "other") {
                                    groups.getOrPut(groupName) { mutableListOf() }.add(entry)
                                } else {
                                    other.add(entry)
                                }
                                currentComments = mutableListOf()
                            }
                        }

                        val sortedGroupNames = groups.keys.sorted()
                        sortedGroupNames.forEachIndexed { index, name ->
                            val sortedEntries = groups[name]!!.sortedBy { it.trim().lowercase() }
                            newLines.addAll(sortedEntries)
                            if (index < sortedGroupNames.size - 1 || other.isNotEmpty()) {
                                if (newLines.last().isNotBlank()) {
                                    newLines.add("")
                                }
                            }
                        }
                        if (other.isNotEmpty()) {
                            newLines.addAll(other.sortedBy { it.trim().lowercase() })
                        }
                    } else {
                        // For plugins, just sort alphabetically but keep comments
                        val entries = mutableListOf<String>()
                        var currentComments = mutableListOf<String>()
                        blockLines.forEach { bl ->
                            val trimmed = bl.trim()
                            if (trimmed.isEmpty()) return@forEach
                            if (trimmed.startsWith("//")) {
                                currentComments.add(bl)
                            } else {
                                entries.add((currentComments + bl).joinToString("\n"))
                                currentComments = mutableListOf()
                            }
                        }
                        newLines.addAll(entries.sortedBy { it.trim().lowercase() })
                    }

                    if (i < lines.size) newLines.add(lines[i])
                } else {
                    newLines.add(line)
                }
                i++
            }
            file.writeText(newLines.joinToString("\n") + "\n")
        }
    }
}

tasks.register<JacocoReport>("createJacocoMergedCoverageReport") {
    group = "Reporting"
    description = "Generates a merged Jacoco code coverage report for all modules."

    val modulesToInclude =
        listOf(
            ":app",
            ":core",
            ":wear",
        )

    dependsOn(modulesToInclude.map { "$it:createJacocoDebugCoverageReport" })

    sourceDirectories.setFrom(
        files(
            subprojects.flatMap {
                listOf("${it.projectDir}/src/main/java", "${it.projectDir}/src/main/kotlin")
            },
        ),
    )

    classDirectories.setFrom(
        files(
            subprojects.flatMap { sp ->
                listOf(
                    fileTree(sp.layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                        exclude(
                            "**/R.class",
                            "**/R$*.class",
                            "**/BuildConfig.*",
                            "**/Manifest*.*",
                            "**/*Test*.*",
                            "android/**/*.*",
                            "**/*_Hilt*.class",
                            "**/Dagger*Component.class",
                            "**/Dagger*Module.class",
                            "**/Dagger*Module_Provide*Factory.class",
                            "**/*_Provide*Factory*.*",
                            "**/*_Factory*.*",
                        )
                    },
                    fileTree(sp.layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
                        exclude(
                            "**/R.class",
                            "**/R$*.class",
                            "**/BuildConfig.*",
                            "**/Manifest*.*",
                            "**/*Test*.*",
                            "android/**/*.*",
                            "**/*_Hilt*.class",
                            "**/Dagger*Component.class",
                            "**/Dagger*Module.class",
                            "**/Dagger*Module_Provide*Factory.class",
                            "**/*_Provide*Factory*.*",
                            "**/*_Factory*.*",
                        )
                    },
                )
            },
        ),
    )

    executionData.setFrom(
        files(
            subprojects.flatMap { sp ->
                listOf(
                    fileTree(sp.layout.buildDirectory) { include("jacoco/testDebugUnitTest.exec") },
                    fileTree(
                        sp.layout.buildDirectory,
                    ) { include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec") },
                )
            },
        ),
    )

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file(
                "reports/jacoco/createJacocoMergedCoverageReport/createJacocoMergedCoverageReport.xml",
            ),
        )
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/createJacocoMergedCoverageReport/html"))
    }
}
