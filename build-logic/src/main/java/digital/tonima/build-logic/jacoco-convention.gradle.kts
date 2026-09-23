plugins {
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.11"
}

// Ensure unit tests generate JaCoCo execution data
tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("createJacocoDebugCoverageReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file(
                "reports/jacoco/createJacocoDebugCoverageReport/createJacocoDebugCoverageReport.xml",
            ),
        )
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/createJacocoDebugCoverageReport/html"))
    }

    val fileFilter =
        listOf(
            // Android
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*", // DI, generated code
            "hilt_aggregated_deps/**",
            "dagger/**",
            "**/Hilt_*.class",
            "**/*_Hilt*.class",
            "**/*_HiltModules*.class",
            "**/*_MembersInjector.class",
            "**/Dagger*Component.class",
            "**/Dagger*Module.class",
            "**/Dagger*Module_Provide*Factory.class",
            "**/*_Provide*Factory*.*",
            "**/*_Factory*.*",
        )

    // Read the classes after Hilt's bytecode transform: that is what the unit tests execute, and the
    // raw compiler output has different class IDs, which makes JaCoCo report 0% coverage.
    val debugClassesTree =
        fileTree(layout.buildDirectory.dir("intermediates/classes/debug/transformDebugClassesWithAsm/dirs")) {
            exclude(fileFilter)
        }

    val mainSrcJava = "${project.projectDir}/src/main/java"
    val mainSrcKotlin = "${project.projectDir}/src/main/kotlin"

    sourceDirectories.setFrom(files(mainSrcJava, mainSrcKotlin))
    classDirectories.setFrom(files(debugClassesTree))
    executionData.setFrom(
        files(
            fileTree(layout.buildDirectory) { include("jacoco/testDebugUnitTest.exec") },
            fileTree(
                layout.buildDirectory,
            ) { include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec") },
        ),
    )
}
