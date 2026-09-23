package digital.tonima.core.permissions

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class PermissionManagerImplTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val manager = PermissionManagerImpl(app)

    @Test
    fun `calendar access needs both read and write`() {
        assertFalse(manager.hasCalendarPermission())

        grant(Manifest.permission.READ_CALENDAR)
        assertFalse("Read alone is not enough to manage alarms", manager.hasCalendarPermission())

        grant(Manifest.permission.WRITE_CALENDAR)
        assertTrue(manager.hasCalendarPermission())
    }

    @Test
    fun `missing standard permissions lists what still has to be asked`() {
        assertEquals(
            listOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.POST_NOTIFICATIONS,
            ),
            manager.getMissingStandardPermissions(),
        )

        grant(Manifest.permission.READ_CALENDAR, Manifest.permission.POST_NOTIFICATIONS)

        assertEquals(listOf(Manifest.permission.WRITE_CALENDAR), manager.getMissingStandardPermissions())
    }

    @Test
    fun `notification permission is required from Android 13`() {
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), manager.notificationPermissions)
        assertFalse(manager.hasPostNotificationsPermission())

        grant(Manifest.permission.POST_NOTIFICATIONS)

        assertTrue(manager.hasPostNotificationsPermission())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `before Android 13 notifications need no runtime permission`() {
        val legacy = PermissionManagerImpl(app)

        assertTrue(legacy.notificationPermissions.isEmpty())
        assertTrue(legacy.hasPostNotificationsPermission())
        assertFalse(Manifest.permission.POST_NOTIFICATIONS in legacy.getMissingStandardPermissions())
    }

    @Test
    fun `either precise or approximate location is enough`() {
        assertFalse(manager.hasLocationPermission())

        grant(Manifest.permission.ACCESS_COARSE_LOCATION)

        assertTrue(manager.hasLocationPermission())
        assertTrue(manager.hasBackgroundLocationPermission())
    }

    @Test
    fun `exact alarms follow the system setting`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(manager.hasExactAlarmPermission())
        assertTrue(manager.needsExactAlarmPermissionRequest())

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertTrue(manager.hasExactAlarmPermission())
        assertFalse(manager.needsExactAlarmPermissionRequest())
        assertEquals(listOf(Manifest.permission.SCHEDULE_EXACT_ALARM), manager.exactAlarmPermissions)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `before Android 12 exact alarms are always allowed`() {
        val legacy = PermissionManagerImpl(app)

        assertTrue(legacy.hasExactAlarmPermission())
        assertFalse(legacy.needsExactAlarmPermissionRequest())
        assertTrue(legacy.exactAlarmPermissions.isEmpty())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `from Android 14 full screen alarms follow the system setting`() {
        val systemAllows = app.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

        assertEquals(systemAllows, manager.hasFullScreenIntentPermission())
        assertEquals(!systemAllows, manager.needsFullScreenIntentPermissionRequest())
        assertEquals(listOf(Manifest.permission.USE_FULL_SCREEN_INTENT), manager.fullScreenIntentPermissions)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `before Android 14 full screen alarms need no permission`() {
        val legacy = PermissionManagerImpl(app)

        assertTrue(legacy.hasFullScreenIntentPermission())
        assertFalse(legacy.needsFullScreenIntentPermissionRequest())
        assertTrue(legacy.fullScreenIntentPermissions.isEmpty())
    }

    private fun grant(vararg permissions: String) = shadowOf(app).grantPermissions(*permissions)
}
