package digital.tonima.core.service

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class EventAlarmSchedulerImplTest {
    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var mockPrefsRepo: AppPreferencesRepository

    // Fixed clock at epoch (1970-01-01 00:00 UTC) — all future test alarm times are always in the future
    private val fixedClock: Clock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault())

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        mockPrefsRepo = mockk<AppPreferencesRepository>()

        stubSchedulerPreferences()
    }

    private fun stubSchedulerPreferences(
        allDayAlarmsEnabled: Boolean = true,
        allDayAlarmHour: Int = 9,
        alarmOffsetMinutes: Long = 0L,
        skipWeekends: Boolean = false,
        snoozeTimeMinutes: Int = 10,
    ) {
        every { mockPrefsRepo.isAllDayAlarmsEnabled() } returns flowOf(allDayAlarmsEnabled)
        every { mockPrefsRepo.getAllDayAlarmHour() } returns flowOf(allDayAlarmHour)
        every { mockPrefsRepo.getAlarmOffsetMinutes() } returns flowOf(alarmOffsetMinutes)
        every { mockPrefsRepo.isSkipWeekendsEnabled() } returns flowOf(skipWeekends)
        every { mockPrefsRepo.getSnoozeTimeMinutes() } returns flowOf(snoozeTimeMinutes)
    }

    private fun createScheduler() =
        EventAlarmSchedulerImpl(context, mockPrefsRepo).apply {
            clock = fixedClock
        }

    @Test
    fun `schedule should set exact alarm with correct trigger time`() {
        val event = Event(id = 10L, title = "T", startTime = 123456789L)

        val scheduler = createScheduler()
        scheduler.schedule(event)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val alarms = shadowAlarmManager.scheduledAlarms
        assert(alarms.isNotEmpty())
        val alarm = alarms.first()
        assert(alarm.type == AlarmManager.RTC_WAKEUP)
        assert(alarm.triggerAtTime == event.startTime)
    }

    @Test
    fun `cancel should cancel the existing alarm`() {
        val event = Event(id = 10L, title = "T", startTime = 123456789L)
        val scheduler = createScheduler()

        scheduler.schedule(event)
        scheduler.cancel(event)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assert(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `schedule should not set alarm for all-day events when disabled`() {
        stubSchedulerPreferences(allDayAlarmsEnabled = false)

        val utcMidnight = Instant.parse("2100-06-15T00:00:00Z").toEpochMilli()
        val allDayEvent = Event(id = 10L, title = "All Day Event", startTime = utcMidnight, isAllDay = true)

        val scheduler = createScheduler()
        scheduler.schedule(allDayEvent)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assert(shadowAlarmManager.scheduledAlarms.isEmpty()) {
            "No alarm should be scheduled when all-day alarms are disabled"
        }
    }

    @Test
    fun `schedule should set alarm for all-day events at configured hour when enabled`() {
        stubSchedulerPreferences(allDayAlarmsEnabled = true, allDayAlarmHour = 9)

        val utcMidnight = Instant.parse("2100-06-15T00:00:00Z").toEpochMilli()
        val allDayEvent = Event(id = 10L, title = "All Day Event", startTime = utcMidnight, isAllDay = true)

        val scheduler = createScheduler()
        scheduler.schedule(allDayEvent)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val alarms = shadowAlarmManager.scheduledAlarms
        assert(alarms.isNotEmpty()) { "Alarm should be scheduled for all-day events when enabled" }
        val alarm = alarms.first()
        assert(alarm.type == AlarmManager.RTC_WAKEUP)

        val expectedAlarmTime =
            Instant.parse("2100-06-15T00:00:00Z")
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()
                .atTime(LocalTime.of(9, 0))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        assert(alarm.triggerAtTime == expectedAlarmTime) {
            "Expected alarm at $expectedAlarmTime but got ${alarm.triggerAtTime}"
        }
    }

    @Test
    fun `schedule should not set alarm for weekend events when skip weekends is enabled`() {
        stubSchedulerPreferences(skipWeekends = true)

        val saturdayStartTime =
            LocalDate.of(2100, 6, 19)
                .atTime(10, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val weekendEvent = Event(id = 11L, title = "Weekend Event", startTime = saturdayStartTime)

        createScheduler().schedule(weekendEvent)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assert(shadowAlarmManager.scheduledAlarms.isEmpty()) {
            "No alarm should be scheduled for weekend events when skip weekends is enabled"
        }
    }

    @Test
    fun `schedule should still set alarm for weekday events when skip weekends is enabled`() {
        stubSchedulerPreferences(skipWeekends = true)

        val mondayStartTime =
            LocalDate.of(2100, 6, 21)
                .atTime(10, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val weekdayEvent = Event(id = 12L, title = "Weekday Event", startTime = mondayStartTime)

        createScheduler().schedule(weekdayEvent)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val alarms = shadowAlarmManager.scheduledAlarms
        assert(alarms.size == 1) { "Alarm should be scheduled for weekday events" }
        assert(alarms.first().triggerAtTime == mondayStartTime)
    }
}
