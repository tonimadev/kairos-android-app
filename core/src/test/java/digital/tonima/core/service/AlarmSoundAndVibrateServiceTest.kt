package digital.tonima.core.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.receiver.AlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmSoundAndVibrateServiceTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `should create PendingIntent with correct flags`() {
        val intent = Intent(context, AlarmSoundAndVibrateService::class.java)
        val pendingIntent =
            PendingIntent.getService(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        assertNotNull(pendingIntent)
    }

    @Test
    fun `alarm blocked from starting in the background posts a full screen fallback instead of crashing`() {
        val blockedContext =
            object : ContextWrapper(context) {
                override fun startForegroundService(service: Intent): ComponentName =
                    throw ForegroundServiceStartNotAllowedException("startForegroundService() not allowed")

                // :core unit tests run without Android resources, so string lookups are stubbed.
                override fun getResources(): Resources =
                    object : Resources(
                        context.assets,
                        context.resources.displayMetrics,
                        context.resources.configuration,
                    ) {
                        override fun getString(id: Int): String = "Alarm"
                    }
            }

        AlarmSoundAndVibrateService.startAlarm(
            context = blockedContext,
            eventTitle = "Standup",
            uniqueId = 42,
            eventId = 7L,
            startTime = 1_000L,
            meetingUrl = "https://meet.example/abc",
        )

        val nm = context.getSystemService(NotificationManager::class.java)
        val posted = shadowOf(nm).getNotification(AlarmSoundAndVibrateService.NOTIFICATION_ID)
        assertNotNull("A fallback alarm notification must be posted", posted)
        assertEquals(AlarmFallbackNotification.CHANNEL_ID, posted.channelId)
        assertNotNull(nm.getNotificationChannel(AlarmFallbackNotification.CHANNEL_ID).sound)

        val opened = shadowOf(posted.fullScreenIntent).savedIntent
        assertEquals("digital.tonima.kairos.ui.view.AlarmActivity", opened.component?.className)
        assertTrue(opened.getBooleanExtra(AlarmSoundAndVibrateService.EXTRA_START_ALARM_SOUND, false))
        assertEquals("Standup", opened.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE))
        assertEquals(42, opened.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1))
        assertEquals(7L, opened.getLongExtra(AlarmReceiver.EXTRA_EVENT_ID, -1L))
        assertEquals("https://meet.example/abc", opened.getStringExtra(AlarmReceiver.EXTRA_MEETING_URL))
    }

    @Test(expected = IllegalStateException::class)
    fun `unrelated failures starting the alarm service are not swallowed`() {
        val brokenContext =
            object : ContextWrapper(context) {
                override fun startForegroundService(service: Intent): ComponentName =
                    throw IllegalStateException("something else")
            }

        AlarmSoundAndVibrateService.startAlarm(context = brokenContext, eventTitle = "Standup", uniqueId = 1)
    }
}
