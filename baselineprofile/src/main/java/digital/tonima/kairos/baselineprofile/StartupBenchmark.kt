package digital.tonima.kairos.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold startup time (time to initial display), without and with the Baseline Profile.
 *
 * ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 * Results: baselineprofile/build/outputs/connected_android_test_additional_output/.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupWithoutPrecompilation() = startup(CompilationMode.None())

    @Test
    fun startupWithBaselineProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    @OptIn(ExperimentalMetricApi::class)
    private fun startup(compilationMode: CompilationMode) =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(
                    StartupTimingMetric(),
                    // Application.onCreate (incl. Hilt graph) and MainActivity.onCreate, to see where startup goes.
                    TraceSectionMetric("bindApplication"),
                    TraceSectionMetric("activityStart"),
                ),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = ITERATIONS,
            setupBlock = { grantStartupPermissions() },
        ) {
            startAndWaitForHome()
        }

    private companion object {
        const val ITERATIONS = 10
    }
}
