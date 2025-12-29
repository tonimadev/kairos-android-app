package digital.tonima.core.service

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class EventAlarmSchedulerImplTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var mockPrefsRepo: AppPreferencesRepository

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        mockPrefsRepo = mockk<AppPreferencesRepository>()

        coEvery { mockPrefsRepo.isAllDayAlarmsEnabled() } returns flowOf(true)
        coEvery { mockPrefsRepo.getAllDayAlarmHour() } returns flowOf(9)
    }

    @Test
    fun `schedule should set exact alarm with correct trigger time`() {
        val event = Event(id = 10L, title = "T", startTime = 123456789L)

        val scheduler = EventAlarmSchedulerImpl(context, mockPrefsRepo)
        scheduler.schedule(event)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val next = shadowAlarmManager.nextScheduledAlarm
        assert(next != null)
        assert(next!!.type == AlarmManager.RTC_WAKEUP)
        assert(next.triggerAtTime == event.startTime)
    }

    @Test
    fun `cancel should cancel the existing alarm`() {
        val event = Event(id = 10L, title = "T", startTime = 123456789L)
        val scheduler = EventAlarmSchedulerImpl(context, mockPrefsRepo)

        scheduler.schedule(event)
        scheduler.cancel(event)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        assert(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `schedule should not set alarm for all-day events when disabled`() {
        coEvery { mockPrefsRepo.isAllDayAlarmsEnabled() } returns flowOf(false)

        val utcMidnight = Instant.parse("2024-01-15T00:00:00Z").toEpochMilli()
        val allDayEvent = Event(id = 10L, title = "All Day Event", startTime = utcMidnight, isAllDay = true)

        val scheduler = EventAlarmSchedulerImpl(context, mockPrefsRepo)
        scheduler.schedule(allDayEvent)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assert(shadowAlarmManager.scheduledAlarms.isEmpty()) { "No alarm should be scheduled when all-day alarms are disabled" }
    }

    @Test
    fun `schedule should set alarm for all-day events at configured hour when enabled`() {
        coEvery { mockPrefsRepo.isAllDayAlarmsEnabled() } returns flowOf(true)
        coEvery { mockPrefsRepo.getAllDayAlarmHour() } returns flowOf(9)

        val utcMidnight = Instant.parse("2024-01-15T00:00:00Z").toEpochMilli()
        val allDayEvent = Event(id = 10L, title = "All Day Event", startTime = utcMidnight, isAllDay = true)

        val scheduler = EventAlarmSchedulerImpl(context, mockPrefsRepo)
        scheduler.schedule(allDayEvent)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val next = shadowAlarmManager.nextScheduledAlarm

        assert(next != null) { "Alarm should be scheduled for all-day events when enabled" }
        assert(next!!.type == AlarmManager.RTC_WAKEUP)

        val expectedAlarmTime = Instant.parse("2024-01-15T00:00:00Z")
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
            .atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assert(next.triggerAtTime == expectedAlarmTime) {
            "Expected alarm at $expectedAlarmTime but got ${next.triggerAtTime}"
        }
    }
}
