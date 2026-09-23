package digital.tonima.core.service

import android.app.Application
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Looper
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.internal.builders.ServiceComponentBuilder
import dagger.hilt.android.internal.managers.ServiceComponentManager
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import digital.tonima.core.analytics.Analytics
import digital.tonima.core.receiver.AlarmReceiver
import digital.tonima.core.repository.AppPreferencesRepositoryImpl
import digital.tonima.core.sync.WearMessagingHelper
import digital.tonima.kairos.core.R
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.time.Duration

/** Resolves the service's Hilt injection to test doubles. */
class AlarmServiceTestApp :
    Application(),
    GeneratedComponentManager<Any> {
    lateinit var analytics: Analytics
    lateinit var wearMessagingHelper: WearMessagingHelper

    private val serviceComponent =
        object : ServiceComponent, GeneratedComponent, AlarmSoundAndVibrateService_GeneratedInjector {
            override fun injectAlarmSoundAndVibrateService(service: AlarmSoundAndVibrateService) {
                service.analytics = analytics
                service.wearMessagingHelper = wearMessagingHelper
            }
        }

    override fun generatedComponent(): Any =
        object : GeneratedComponent, ServiceComponentManager.ServiceComponentBuilderEntryPoint {
            override fun serviceComponentBuilder(): ServiceComponentBuilder =
                object : ServiceComponentBuilder {
                    override fun service(service: Service): ServiceComponentBuilder = this

                    override fun build(): ServiceComponent = serviceComponent
                }
        }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = AlarmServiceTestApp::class)
class AlarmSoundAndVibrateServiceTest {
    private lateinit var app: AlarmServiceTestApp
    private val analytics: Analytics = mockk(relaxed = true)
    private val wearMessagingHelper: WearMessagingHelper = mockk(relaxed = true)

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.analytics = analytics
        app.wearMessagingHelper = wearMessagingHelper
    }

    // region Start

    @Test
    fun `starting the alarm shows an ongoing full-screen alarm notification`() {
        val service = start(startIntent()).get()

        val notification = shadowOf(service).lastForegroundNotification
        assertNotNull("The alarm must run as a foreground service", notification)
        assertEquals(AlarmSoundAndVibrateService.NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        assertTrue(
            "Alarm notification must not be swipeable",
            notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
        )
        assertNotNull("The alarm screen must open over the lock screen", notification.fullScreenIntent)
        assertEquals("Daily standup", notification.extras.getString(Notification.EXTRA_TEXT))
    }

    @Test
    fun `alarm notification offers snooze and stop, plus join and map when available`() {
        val withEverything = start(startIntent()).get()
        assertEquals(
            listOf(
                R.string.snooze,
                R.string.stop,
                R.string.join_meeting_label,
                R.string.open_map_label,
            ).map(app::getString),
            actionTitles(withEverything),
        )

        val plain =
            start(
                startIntent().apply {
                    removeExtra(AlarmReceiver.EXTRA_MEETING_URL)
                    removeExtra(AlarmReceiver.EXTRA_EVENT_LOCATION)
                },
            ).get()
        assertEquals(listOf(R.string.snooze, R.string.stop).map(app::getString), actionTitles(plain))
    }

    @Test
    fun `snooze action carries the event so it can be rescheduled`() {
        val service = start(startIntent()).get()

        val snooze = shadowOf(shadowOf(service).lastForegroundNotification.actions[0].actionIntent).savedIntent
        assertEquals(AlarmReceiver.ACTION_SNOOZE, snooze.action)
        assertEquals(UNIQUE_ID, snooze.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1))
        assertEquals(EVENT_ID, snooze.getLongExtra(AlarmReceiver.EXTRA_EVENT_ID, -1L))
        assertEquals("Daily standup", snooze.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE))
    }

    @Test
    fun `snooze action and alarm screen keep the location and end time`() {
        val service = start(startIntent().putExtra(AlarmReceiver.EXTRA_EVENT_END_TIME, 1_800_001_800_000L)).get()
        val notification = shadowOf(service).lastForegroundNotification

        listOf(
            shadowOf(notification.actions[0].actionIntent).savedIntent,
            shadowOf(notification.fullScreenIntent).savedIntent,
        ).forEach { intent ->
            assertEquals("Room 42", intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_LOCATION))
            assertEquals(1_800_001_800_000L, intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_END_TIME, -1L))
            assertEquals("https://meet.google.com/abc", intent.getStringExtra(AlarmReceiver.EXTRA_MEETING_URL))
        }
    }

    @Test
    fun `alarm without title falls back to the generic event text`() {
        val service = start(startIntent().apply { removeExtra(AlarmReceiver.EXTRA_EVENT_TITLE) }).get()

        assertEquals(
            app.getString(R.string.upcoming_event),
            shadowOf(service).lastForegroundNotification.extras.getString(Notification.EXTRA_TEXT),
        )
    }

    @Test
    fun `starting the alarm vibrates`() {
        start(startIntent())

        assertTrue(shadowOf(defaultVibrator()).isVibrating)
    }

    // endregion

    // region Stop

    @Test
    fun `stopping from the notification stops everything and tells the watch`() {
        val controller = start(startIntent())

        controller.withIntent(stopIntent(Analytics.SOURCE_NOTIFICATION)).startCommand(0, 2)
        shadowOf(Looper.getMainLooper()).idle()

        val service = controller.get()
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertTrue(shadowOf(service).isForegroundStopped)
        assertFalse(shadowOf(defaultVibrator()).isVibrating)
        verify {
            analytics.logEvent(
                Analytics.EVENT_ALARM_STOP,
                mapOf(Analytics.PARAM_SOURCE to Analytics.SOURCE_NOTIFICATION),
            )
        }
        coVerify { wearMessagingHelper.sendDismissAlarm(UNIQUE_ID) }
    }

    @Test
    fun `stop requested by the other device is not echoed back`() {
        listOf("PHONE_SYNC", "WEAR_SYNC").forEach { source ->
            val controller = start(startIntent())

            controller.withIntent(stopIntent(source)).startCommand(0, 2)
            shadowOf(Looper.getMainLooper()).idle()

            assertTrue(shadowOf(controller.get()).isStoppedBySelf)
        }
        coVerify(exactly = 0) { wearMessagingHelper.sendDismissAlarm(any()) }
        verify(exactly = 0) { analytics.logEvent(Analytics.EVENT_ALARM_STOP, any()) }
    }

    @Test
    fun `destroying the service stops the vibration`() {
        val controller = start(startIntent())

        controller.destroy()

        assertFalse(shadowOf(defaultVibrator()).isVibrating)
    }

    // endregion

    // region Auto dismiss

    @Test
    fun `alarm is dismissed automatically after the configured minutes`() {
        runBlocking { AppPreferencesRepositoryImpl(app).setAutoDismissMinutes(3) }
        start(startIntent())
        clearStartedServices()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(2))
        assertEquals("Must still be ringing before the timeout", null, shadowOf(app).nextStartedService)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(1).plusSeconds(1))
        val stop = shadowOf(app).nextStartedService
        assertNotNull("The alarm must stop by itself", stop)
        assertEquals(AlarmSoundAndVibrateService.ACTION_STOP_ALARM, stop!!.action)
        assertEquals("AUTO_DISMISS", stop.getStringExtra(AlarmSoundAndVibrateService.EXTRA_SOURCE))
        assertEquals(UNIQUE_ID, stop.getIntExtra(AlarmSoundAndVibrateService.EXTRA_UNIQUE_ID, -1))
        assertTrue(
            shadowOf(
                app,
            ).broadcastIntents.any { it.action == AlarmSoundAndVibrateService.ACTION_FINISH_ALARM_ACTIVITY },
        )
    }

    @Test
    fun `stopping the alarm cancels the pending auto dismiss`() {
        runBlocking { AppPreferencesRepositoryImpl(app).setAutoDismissMinutes(1) }
        val controller = start(startIntent())
        controller.withIntent(stopIntent("PHONE_SYNC")).startCommand(0, 2)
        clearStartedServices()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(5))

        assertEquals(null, shadowOf(app).nextStartedService)
    }

    // endregion

    private fun start(intent: Intent): ServiceController<AlarmSoundAndVibrateService> {
        val controller =
            Robolectric.buildService(
                AlarmSoundAndVibrateService::class.java,
                intent,
            ).create().startCommand(0, 1)
        shadowOf(Looper.getMainLooper()).idle()
        return controller
    }

    private fun startIntent() =
        Intent(app, AlarmSoundAndVibrateService::class.java).apply {
            action = AlarmSoundAndVibrateService.ACTION_START_ALARM
            putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, "Daily standup")
            putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, UNIQUE_ID)
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, EVENT_ID)
            putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, 1_800_000_000_000L)
            putExtra(AlarmReceiver.EXTRA_MEETING_URL, "https://meet.google.com/abc")
            putExtra(AlarmReceiver.EXTRA_EVENT_LOCATION, "Room 42")
        }

    private fun stopIntent(source: String) =
        Intent(app, AlarmSoundAndVibrateService::class.java).apply {
            action = AlarmSoundAndVibrateService.ACTION_STOP_ALARM
            putExtra(AlarmSoundAndVibrateService.EXTRA_SOURCE, source)
            putExtra(AlarmSoundAndVibrateService.EXTRA_UNIQUE_ID, UNIQUE_ID)
        }

    private fun actionTitles(service: AlarmSoundAndVibrateService) =
        shadowOf(service).lastForegroundNotification.actions.map { it.title.toString() }

    private fun defaultVibrator() = app.getSystemService(VibratorManager::class.java).defaultVibrator

    private fun clearStartedServices() {
        while (shadowOf(app).nextStartedService != null) Unit
    }

    private companion object {
        const val UNIQUE_ID = 4242
        const val EVENT_ID = 77L
    }
}
