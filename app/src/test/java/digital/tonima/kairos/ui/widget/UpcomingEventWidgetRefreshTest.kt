package digital.tonima.kairos.ui.widget

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpcomingEventWidgetRefreshTest {
    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
    }

    @Test
    fun `the next refresh is an inexact alarm that does not wake the device`() {
        UpcomingEventWidgetRefresh.schedule(context, TRIGGER_AT)

        val alarm = scheduledAlarm()
        assertEquals(AlarmManager.RTC, alarm.type)
        assertEquals(TRIGGER_AT, alarm.triggerAtTime)
    }

    @Test
    fun `scheduling again replaces the previous refresh instead of adding another`() {
        UpcomingEventWidgetRefresh.schedule(context, TRIGGER_AT)
        UpcomingEventWidgetRefresh.schedule(context, TRIGGER_AT + 60_000L)

        assertEquals(TRIGGER_AT + 60_000L, scheduledAlarm().triggerAtTime)
        // A single cancel clears it, so no second alarm was left behind.
        UpcomingEventWidgetRefresh.cancel(context)
        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }

    @Test
    fun `without a next event the pending refresh is cancelled`() {
        UpcomingEventWidgetRefresh.schedule(context, TRIGGER_AT)

        UpcomingEventWidgetRefresh.schedule(context, null)

        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }

    @Test
    fun `the refresh alarm targets the widget receiver`() {
        UpcomingEventWidgetRefresh.schedule(context, TRIGGER_AT)

        val intent = shadowOf(scheduledAlarm().operation).savedIntent
        assertEquals(UpcomingEventWidgetRefresh.ACTION_REFRESH, intent.action)
        assertEquals(ComponentName(context, UpcomingEventWidgetReceiver::class.java), intent.component)
    }

    @Test
    fun `refreshing now does nothing when no widget is placed`() {
        UpcomingEventWidgetRefresh.refreshNow(context)

        assertTrue(shadowOf(context as android.app.Application).broadcastIntents.isEmpty())
    }

    private fun scheduledAlarm() = requireNotNull(shadowOf(alarmManager).nextScheduledAlarm)

    private companion object {
        const val TRIGGER_AT = 1_790_000_000_000L
    }
}
