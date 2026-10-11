package digital.tonima.kairos.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's Baseline Profile for the startup journey.
 *
 * Run on a rooted emulator or API 33+ device:
 * ./gradlew :app:generateBaselineProfile
 * The profile lands in app/src/release/generated/baselineProfiles/.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(
            packageName = PACKAGE_NAME,
            includeInStartupProfile = true,
            // The startup journey is deterministic; a few iterations reach a stable profile.
            maxIterations = 8,
            stableIterations = 2,
        ) {
            grantStartupPermissions()
            startAndWaitForHome()
        }
}
