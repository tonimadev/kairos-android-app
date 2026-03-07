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
            "**/*_Hilt*.class",
            "**/Dagger*Component.class",
            "**/Dagger*Module.class",
            "**/Dagger*Module_Provide*Factory.class",
            "**/*_Provide*Factory*.*",
            "**/*_Factory*.*",
        )

    val kotlinDebugTree = fileTree("${layout.buildDirectory}/tmp/kotlin-classes/debug") { exclude(fileFilter) }
    val javaDebugTree = fileTree("${layout.buildDirectory}/intermediates/javac/debug/classes") { exclude(fileFilter) }

    val mainSrcJava = "${project.projectDir}/src/main/java"
    val mainSrcKotlin = "${project.projectDir}/src/main/kotlin"

    sourceDirectories.setFrom(files(mainSrcJava, mainSrcKotlin))
    classDirectories.setFrom(files(kotlinDebugTree, javaDebugTree))
    executionData.setFrom(
        files(
            fileTree(layout.buildDirectory) { include("jacoco/testDebugUnitTest.exec") },
            fileTree(
                layout.buildDirectory,
            ) { include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec") },
        ),
    )
}
