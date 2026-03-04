package digital.tonima.kairos.wear.service

import android.app.Application
import android.content.Context
import android.content.Intent
import digital.tonima.core.receiver.AlarmReceiver.Companion.ACTION_ALARM_TRIGGERED
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_TITLE
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_UNIQUE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WearAlarmReceiverTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication() as Application
    }

    @Test
    fun `onReceive starts foreground service with event title extra`() {
        val eventTitle = "Team Sync"
        val uniqueId = 12345 // kept for parity with previous test, not used in new behavior

        val intent =
            Intent(ACTION_ALARM_TRIGGERED).apply {
                putExtra(EXTRA_EVENT_TITLE, eventTitle)
                putExtra(EXTRA_UNIQUE_ID, uniqueId)
                `package` = context.packageName
            }

        val receiver = WearAlarmReceiver()
        receiver.onReceive(context, intent)

        val shadowApp = shadowOf(context as Application)
        val startedIntent = shadowApp.nextStartedService

        assertNotNull("Alarm service should be started", startedIntent)
        // Verify service and action
        assertEquals(
            digital.tonima.core.service.AlarmSoundAndVibrateService::class.java.name,
            startedIntent!!.component?.className,
        )
        assertEquals(
            digital.tonima.core.service.AlarmSoundAndVibrateService.ACTION_START_ALARM,
            startedIntent.action,
        )
        // Verify the title extra was passed along
        assertEquals(eventTitle, startedIntent.getStringExtra(EXTRA_EVENT_TITLE))
    }
}
