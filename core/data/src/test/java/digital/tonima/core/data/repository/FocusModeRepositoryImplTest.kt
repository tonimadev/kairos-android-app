package digital.tonima.core.data.repository

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FocusModeRepositoryImplTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val repository = FocusModeRepositoryImpl(context)

    @Test
    fun `do not disturb is left alone without policy access`() {
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(false)

        repository.setDoNotDisturb(true)

        assertFalse(repository.hasNotificationPolicyAccess())
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, notificationManager.currentInterruptionFilter)
        assertFalse(repository.isDoNotDisturbEnabled())
    }

    @Test
    fun `do not disturb is switched to priority only and back`() {
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(true)

        repository.setDoNotDisturb(true)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, notificationManager.currentInterruptionFilter)
        assertTrue(repository.isDoNotDisturbEnabled())

        repository.setDoNotDisturb(false)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, notificationManager.currentInterruptionFilter)
        assertFalse(repository.isDoNotDisturbEnabled())
    }
}
