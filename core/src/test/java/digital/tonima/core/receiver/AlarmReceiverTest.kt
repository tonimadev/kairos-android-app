package digital.tonima.core.receiver

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import digital.tonima.core.analytics.Analytics
import digital.tonima.core.analytics.CrashReporter
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.core.service.EventAlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowBroadcastPendingResult
import java.util.concurrent.TimeUnit

/**
 * Stands in for the Hilt application so the receiver's generated `inject()` and its
 * `EntryPointAccessors.fromApplication` lookup resolve to the test doubles below.
 */
class AlarmReceiverTestApp :
    Application(),
    GeneratedComponentManager<Any> {
    lateinit var scheduler: EventAlarmScheduler
    lateinit var analytics: Analytics
    lateinit var preferences: AppPreferencesRepository
    lateinit var crashReporter: CrashReporter

    override fun generatedComponent(): Any =
        object : GeneratedComponent, AlarmReceiver_GeneratedInjector, AlarmReceiver.SchedulerEntryPoint {
            override fun injectAlarmReceiver(alarmReceiver: AlarmReceiver) {
                alarmReceiver.scheduler = scheduler
                alarmReceiver.analytics = analytics
                alarmReceiver.appStatusRepository = preferences
                alarmReceiver.crashReporter = crashReporter
            }

            override fun scheduler(): EventAlarmScheduler = scheduler
        }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = AlarmReceiverTestApp::class)
class AlarmReceiverTest {
    private lateinit var app: AlarmReceiverTestApp
    private lateinit var receiver: AlarmReceiver
    private val scheduler: EventAlarmScheduler = mockk(relaxed = true)
    private val analytics: Analytics = mockk(relaxed = true)
    private val preferences: AppPreferencesRepository = mockk(relaxed = true)
    private val crashReporter: CrashReporter = mockk(relaxed = true)

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.scheduler = scheduler
        app.analytics = analytics
        app.preferences = preferences
        app.crashReporter = crashReporter

        every { preferences.isGlobalAlarmEnabled() } returns flowOf(true)
        every { preferences.getDisabledEventIds() } returns flowOf(emptySet())
        every { preferences.getDisabledSeriesIds() } returns flowOf(emptySet())
        every { preferences.isAutoFocusModeEnabled() } returns flowOf(false)
        every { preferences.isAutoJoinEnabled() } returns flowOf(false)

        receiver = AlarmReceiver()
        app.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(AlarmReceiver.ACTION_ALARM_TRIGGERED)
                addAction(AlarmReceiver.ACTION_SNOOZE)
                addAction(AlarmReceiver.ACTION_JOIN_MEETING)
                addAction(AlarmReceiver.ACTION_OPEN_MAP)
                addAction(AlarmReceiver.ACTION_FOCUS_END)
            },
            Context.RECEIVER_NOT_EXPORTED,
        )
    }

    // region Alarm triggered

    @Test
    fun `alarm triggered starts the alarm service with the event data`() {
        deliver(alarmIntent())

        val started = nextStartedService()
        assertNotNull("Alarm service should be started", started)
        assertEquals(AlarmSoundAndVibrateService.ACTION_START_ALARM, started!!.action)
        assertEquals("Daily standup", started.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE))
        assertEquals(UNIQUE_ID, started.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1))
        assertEquals(EVENT_ID, started.getLongExtra(AlarmReceiver.EXTRA_EVENT_ID, -1L))
        assertEquals(START_TIME, started.getLongExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, -1L))
        assertEquals(MEETING_URL, started.getStringExtra(AlarmReceiver.EXTRA_MEETING_URL))
        assertEquals("Room 42", started.getStringExtra(AlarmReceiver.EXTRA_EVENT_LOCATION))
        verify { analytics.logEvent(Analytics.EVENT_ALARM_FIRED, mapOf(Analytics.PARAM_HAS_MEETING_URL to true)) }
    }

    @Test
    fun `alarm is ignored when alarms are globally disabled`() {
        every { preferences.isGlobalAlarmEnabled() } returns flowOf(false)

        deliver(alarmIntent())

        assertNull("No alarm must ring when alarms are globally disabled", nextStartedService())
    }

    @Test
    fun `alarm is ignored when this occurrence was disabled`() {
        every { preferences.getDisabledEventIds() } returns flowOf(setOf(UNIQUE_ID.toString()))

        deliver(alarmIntent())

        assertNull(nextStartedService())
    }

    @Test
    fun `alarm is ignored when the whole recurring series was disabled`() {
        every { preferences.getDisabledSeriesIds() } returns flowOf(setOf(EVENT_ID.toString()))

        deliver(alarmIntent())

        assertNull(nextStartedService())
    }

    @Test
    fun `alarm still rings when reading the global switch fails`() {
        every { preferences.isGlobalAlarmEnabled() } returns flow { error("disk error") }

        deliver(alarmIntent())

        assertNotNull("A storage failure must never silence an alarm", nextStartedService())
        verify { crashReporter.recordNonFatal(any(), "AlarmReceiver: failed to read global alarm switch") }
    }

    @Test
    fun `alarm still rings when reading the disabled ids fails`() {
        every { preferences.getDisabledEventIds() } returns flow { error("disk error") }

        deliver(alarmIntent())

        assertNotNull(nextStartedService())
    }

    @Test
    fun `other occurrences of a disabled id do not silence this alarm`() {
        every { preferences.getDisabledEventIds() } returns flowOf(setOf("999"))
        every { preferences.getDisabledSeriesIds() } returns flowOf(setOf("888"))

        deliver(alarmIntent())

        assertNotNull(nextStartedService())
    }

    // endregion

    // region Auto join

    @Test
    fun `auto join opens the meeting url instead of ringing`() {
        every { preferences.isAutoJoinEnabled() } returns flowOf(true)

        deliver(alarmIntent())

        val opened = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, opened.action)
        assertEquals(MEETING_URL, opened.dataString)
        assertNull("Auto join must not ring the alarm", nextStartedService())
        verify { analytics.logEvent(Analytics.EVENT_JOIN_MEETING) }
    }

    @Test
    fun `auto join rings normally when the event has no meeting url`() {
        every { preferences.isAutoJoinEnabled() } returns flowOf(true)

        deliver(alarmIntent().apply { removeExtra(AlarmReceiver.EXTRA_MEETING_URL) })

        assertNull(shadowOf(app).nextStartedActivity)
        assertNotNull(nextStartedService())
    }

    // endregion

    // region Auto focus mode

    @Test
    fun `auto focus mode enables do not disturb and schedules its end at the event end`() {
        every { preferences.isAutoFocusModeEnabled() } returns flowOf(true)
        val notificationManager = app.getSystemService(NotificationManager::class.java)
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        deliver(alarmIntent())

        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, notificationManager.currentInterruptionFilter)
        val focusEnd = shadowOf(app.getSystemService(AlarmManager::class.java)).peekNextScheduledAlarm()
        assertNotNull("The end of focus mode must be scheduled", focusEnd)
        assertEquals(END_TIME, focusEnd!!.triggerAtMs)
        assertNotNull("The alarm itself must still ring", nextStartedService())
    }

    @Test
    fun `auto focus mode does nothing without notification policy access`() {
        every { preferences.isAutoFocusModeEnabled() } returns flowOf(true)
        val notificationManager = app.getSystemService(NotificationManager::class.java)
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(false)

        deliver(alarmIntent())

        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, notificationManager.currentInterruptionFilter)
        assertNull(shadowOf(app.getSystemService(AlarmManager::class.java)).peekNextScheduledAlarm())
        assertNotNull(nextStartedService())
    }

    @Test
    fun `auto focus mode is skipped when the event has no end time`() {
        every { preferences.isAutoFocusModeEnabled() } returns flowOf(true)
        val notificationManager = app.getSystemService(NotificationManager::class.java)
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(true)

        deliver(alarmIntent().apply { removeExtra(AlarmReceiver.EXTRA_EVENT_END_TIME) })

        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, notificationManager.currentInterruptionFilter)
    }

    @Test
    fun `focus end restores all notifications`() {
        val notificationManager = app.getSystemService(NotificationManager::class.java)
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(true)
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

        deliver(Intent(AlarmReceiver.ACTION_FOCUS_END))

        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, notificationManager.currentInterruptionFilter)
    }

    // endregion

    // region Notification actions

    @Test
    fun `snooze stops the alarm, reschedules it and counts the snooze`() {
        coEvery { preferences.incrementSnoozeCount() } returns Unit

        deliver(
            Intent(AlarmReceiver.ACTION_SNOOZE).apply {
                putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, "Daily standup")
                putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, UNIQUE_ID)
                putExtra(AlarmReceiver.EXTRA_EVENT_ID, EVENT_ID)
                putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, START_TIME)
                putExtra(AlarmReceiver.EXTRA_MEETING_URL, MEETING_URL)
            },
        )

        assertEquals(AlarmSoundAndVibrateService.ACTION_STOP_ALARM, nextStartedService()!!.action)
        verify { scheduler.scheduleSnooze("Daily standup", UNIQUE_ID, EVENT_ID, START_TIME, MEETING_URL) }
        coVerify { preferences.incrementSnoozeCount() }
        verify { analytics.logEvent(Analytics.EVENT_ALARM_SNOOZE, any()) }
    }

    @Test
    fun `snooze from a non notification source is not logged as a notification snooze`() {
        deliver(
            Intent(AlarmReceiver.ACTION_SNOOZE).apply {
                putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, UNIQUE_ID)
                putExtra(AlarmReceiver.EXTRA_SOURCE, "ALARM_SCREEN")
            },
        )

        verify(exactly = 0) { analytics.logEvent(Analytics.EVENT_ALARM_SNOOZE, any()) }
        verify { scheduler.scheduleSnooze("", UNIQUE_ID, -1L, -1L, null) }
    }

    @Test
    fun `snooze keeps the location and end time for the next ring`() {
        deliver(
            Intent(AlarmReceiver.ACTION_SNOOZE).apply {
                putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, UNIQUE_ID)
                putExtra(AlarmReceiver.EXTRA_EVENT_LOCATION, "Room 42")
                putExtra(AlarmReceiver.EXTRA_EVENT_END_TIME, END_TIME)
            },
        )

        verify { scheduler.scheduleSnooze(any(), UNIQUE_ID, any(), any(), any(), "Room 42", END_TIME) }
    }

    @Test
    fun `snooze is still rescheduled when counting it fails`() {
        coEvery { preferences.incrementSnoozeCount() } throws IllegalStateException("disk error")

        deliver(Intent(AlarmReceiver.ACTION_SNOOZE).putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, UNIQUE_ID))

        verify { scheduler.scheduleSnooze(any(), UNIQUE_ID, any(), any(), any()) }
    }

    @Test
    fun `join meeting stops the alarm and opens the meeting`() {
        deliver(
            Intent(AlarmReceiver.ACTION_JOIN_MEETING).apply {
                putExtra(AlarmReceiver.EXTRA_MEETING_URL, MEETING_URL)
                putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, UNIQUE_ID)
            },
        )

        val stop = nextStartedService()!!
        assertEquals(AlarmSoundAndVibrateService.ACTION_STOP_ALARM, stop.action)
        assertEquals(UNIQUE_ID, stop.getIntExtra(AlarmSoundAndVibrateService.EXTRA_UNIQUE_ID, -1))
        assertEquals(MEETING_URL, shadowOf(app).nextStartedActivity.dataString)
    }

    @Test
    fun `join meeting without url does nothing`() {
        deliver(Intent(AlarmReceiver.ACTION_JOIN_MEETING))

        assertNull(nextStartedService())
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `open map stops the alarm and starts google maps navigation`() {
        deliver(
            Intent(AlarmReceiver.ACTION_OPEN_MAP).apply {
                putExtra(AlarmReceiver.EXTRA_EVENT_LOCATION, "Av. Paulista, 1000")
                putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, UNIQUE_ID)
            },
        )

        assertEquals(AlarmSoundAndVibrateService.ACTION_STOP_ALARM, nextStartedService()!!.action)
        val navigation = shadowOf(app).nextStartedActivity
        assertEquals("com.google.android.apps.maps", navigation.`package`)
        assertEquals("google.navigation:q=Av.%20Paulista%2C%201000", navigation.dataString)
    }

    @Test
    fun `open map without location does nothing`() {
        deliver(Intent(AlarmReceiver.ACTION_OPEN_MAP).putExtra(AlarmReceiver.EXTRA_EVENT_LOCATION, " "))

        assertNull(nextStartedService())
        assertNull(shadowOf(app).nextStartedActivity)
    }

    // endregion

    /**
     * Sends the broadcast through the framework so `goAsync()` gets a real PendingResult, then
     * waits until the receiver's background coroutine calls `finish()` on it.
     */
    private fun deliver(intent: Intent) {
        intent.setPackage(app.packageName)
        app.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()

        val pendingResult: BroadcastReceiver.PendingResult? = shadowOf(receiver).originalPendingResult
        if (shadowOf(receiver).wentAsync() && pendingResult != null) {
            Shadow.extract<ShadowBroadcastPendingResult>(pendingResult).future.get(5, TimeUnit.SECONDS)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun nextStartedService(): Intent? = shadowOf(app).nextStartedService

    private fun alarmIntent() =
        Intent(AlarmReceiver.ACTION_ALARM_TRIGGERED).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, "Daily standup")
            putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, UNIQUE_ID)
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, EVENT_ID)
            putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, START_TIME)
            putExtra(AlarmReceiver.EXTRA_EVENT_END_TIME, END_TIME)
            putExtra(AlarmReceiver.EXTRA_MEETING_URL, MEETING_URL)
            putExtra(AlarmReceiver.EXTRA_EVENT_LOCATION, "Room 42")
        }

    private companion object {
        const val UNIQUE_ID = 4242
        const val EVENT_ID = 77L
        const val START_TIME = 1_800_000_000_000L
        const val END_TIME = START_TIME + 30 * 60_000L
        const val MEETING_URL = "https://meet.google.com/abc-defg-hij"
    }
}
