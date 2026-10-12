package digital.tonima.kairos.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val PACKAGE_NAME = "digital.tonima.kairos"
private const val CONTENT_TIMEOUT_MS = 10_000L

/**
 * Grants what the home screen needs, so startup reaches the real event list instead of the
 * "permissions required" screen (a fresh install has none granted).
 */
fun MacrobenchmarkScope.grantStartupPermissions() {
    // Leave the app first: when the process of the foreground app dies, SystemUI (Android 16+)
    // relaunches its activity, which then races with the measured cold start.
    pressHome()
    listOf(
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.POST_NOTIFICATIONS",
    ).forEach { device.executeShellCommand("pm grant $PACKAGE_NAME $it") }
    // Macrobenchmark wakes the app (shader-cache broadcast) right before its own kill, and the
    // WorkManager jobs that Application.onCreate enqueues then restart the process before the
    // cold start. Background restriction keeps jobs from running while the app is not in front.
    device.executeShellCommand("cmd appops set $PACKAGE_NAME RUN_ANY_IN_BACKGROUND ignore")
    // A permission change can wake the app (receivers, WorkManager); a cold start needs it stopped.
    killProcess()
}

/** Cold start up to the home screen with its content drawn. */
fun MacrobenchmarkScope.startAndWaitForHome() {
    pressHome()
    startActivityAndWait()
    device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), CONTENT_TIMEOUT_MS)
    device.waitForIdle()
    // Leave before the next iteration kills the process (see grantStartupPermissions). Startup
    // metrics come from the trace up to the first frame, so this is not measured.
    pressHome()
}
