package digital.tonima.core.service

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import digital.tonima.core.receiver.AlarmReceiver
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.kairos.core.model.Event
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
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
        registerLauncherActivity()
    }

    /** The real app has a launcher; without one the alarm-clock "show" PendingIntent has no intent. */
    private fun registerLauncherActivity() {
        val launcher = ComponentName(context, "digital.tonima.kairos.FakeLauncherActivity")
        Shadows.shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(launcher)
            addIntentFilterForActivity(
                launcher,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
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

    @Test
    fun `skip weekends uses the UTC date for all-day events`() {
        stubSchedulerPreferences(skipWeekends = true)
        // Saturday at midnight UTC: in UTC-3 this instant is still Friday evening locally,
        // but an all-day event belongs to its calendar date, so it must be skipped anywhere.
        val saturdayUtc = Instant.parse("2100-06-19T00:00:00Z").toEpochMilli()

        createScheduler().schedule(Event(id = 13L, title = "Sat", startTime = saturdayUtc, isAllDay = true))

        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `schedule applies the alarm offset before the event start`() {
        stubSchedulerPreferences(alarmOffsetMinutes = 15L)
        val event = Event(id = 10L, title = "T", startTime = 123_456_789L)

        createScheduler().schedule(event)

        assertEquals(event.startTime - 15 * 60_000L, nextAlarm().triggerAtMs)
    }

    @Test
    fun `explicit trigger time overrides the offset`() {
        stubSchedulerPreferences(alarmOffsetMinutes = 15L)
        val event = Event(id = 10L, title = "T", startTime = 123_456_789L)
        val departure = event.startTime - 45 * 60_000L

        createScheduler().schedule(event, triggerTime = departure)

        assertEquals(departure, nextAlarm().triggerAtMs)
    }

    @Test
    fun `explicit trigger time also overrides the all-day hour`() {
        val allDay =
            Event(
                id = 10L,
                title = "T",
                startTime = Instant.parse("2100-06-15T00:00:00Z").toEpochMilli(),
                isAllDay = true,
            )

        createScheduler().schedule(allDay, triggerTime = 5_000L)

        assertEquals(5_000L, nextAlarm().triggerAtMs)
    }

    @Test
    fun `alarms whose time already passed are not scheduled`() {
        val scheduler =
            EventAlarmSchedulerImpl(context, mockPrefsRepo).apply {
                clock = Clock.fixed(Instant.ofEpochMilli(200_000L), ZoneId.systemDefault())
            }

        scheduler.schedule(Event(id = 1L, title = "Past", startTime = 100_000L))
        scheduler.schedule(Event(id = 2L, title = "Now", startTime = 200_000L))

        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `alarm intent carries everything the receiver needs`() {
        val event =
            Event(
                id = 77L,
                title = "Planning",
                startTime = 123_456_789L,
                endTime = 125_256_789L,
                meetingUrl = "https://meet.google.com/abc",
                location = "Room 42",
            )

        createScheduler().schedule(event)

        val intent = Shadows.shadowOf(nextAlarm().operation).savedIntent
        assertEquals(AlarmReceiver.ACTION_ALARM_TRIGGERED, intent.action)
        assertEquals(AlarmReceiver::class.java.name, intent.component?.className)
        assertEquals("kairos://alarm/${event.uniqueIntentId}", intent.dataString)
        assertEquals("Planning", intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE))
        assertEquals(event.uniqueIntentId, intent.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1))
        assertEquals(77L, intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_ID, -1L))
        assertEquals(event.startTime, intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, -1L))
        assertEquals(event.endTime, intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_END_TIME, -1L))
        assertEquals("https://meet.google.com/abc", intent.getStringExtra(AlarmReceiver.EXTRA_MEETING_URL))
        assertEquals("Room 42", intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_LOCATION))
    }

    @Test
    fun `occurrences of the same recurring event get independent alarms`() {
        val first = Event(id = 5L, title = "Weekly", startTime = 1_000_000L)
        val second = first.copy(startTime = first.startTime + 7 * 24 * 3_600_000L)

        createScheduler().apply {
            schedule(first)
            schedule(second)
        }

        assertEquals(2, Shadows.shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `rescheduling the same event replaces its alarm instead of duplicating it`() {
        val event = Event(id = 5L, title = "T", startTime = 1_000_000L)

        createScheduler().apply {
            schedule(event)
            schedule(event.copy(title = "Renamed"))
        }

        val alarms = Shadows.shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(
            "Renamed",
            Shadows.shadowOf(alarms.first().operation).savedIntent.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE),
        )
    }

    @Test
    fun `cancel only removes the alarm of that occurrence`() {
        val kept = Event(id = 1L, title = "Kept", startTime = 1_000_000L)
        val cancelled = Event(id = 2L, title = "Cancelled", startTime = 2_000_000L)
        val scheduler = createScheduler()
        scheduler.schedule(kept)
        scheduler.schedule(cancelled)

        scheduler.cancel(cancelled)

        val alarms = Shadows.shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(
            kept.uniqueIntentId,
            Shadows.shadowOf(alarms.first().operation).savedIntent.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1),
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `falls back to an inexact alarm when exact alarms are not permitted`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val event = Event(id = 10L, title = "T", startTime = 123_456_789L)

        createScheduler().schedule(event)

        val alarm = nextAlarm()
        assertEquals(event.startTime, alarm.triggerAtMs)
        assertNull("Inexact fallback must not use setAlarmClock", alarm.alarmClockInfo)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `uses an alarm clock when exact alarms are permitted`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        createScheduler().schedule(Event(id = 10L, title = "T", startTime = 123_456_789L))

        assertNotNull(nextAlarm().alarmClockInfo)
    }

    @Test
    fun `snooze fires after the configured snooze time with the event data`() {
        stubSchedulerPreferences(snoozeTimeMinutes = 7)

        createScheduler().scheduleSnooze("Planning", 4242, 77L, 123_456_789L, "https://meet.google.com/abc")

        val alarm = nextAlarm()
        assertEquals(fixedClock.millis() + 7 * 60_000L, alarm.triggerAtMs)
        val intent = Shadows.shadowOf(alarm.operation).savedIntent
        assertEquals(AlarmReceiver.ACTION_ALARM_TRIGGERED, intent.action)
        assertEquals("kairos://alarm/4242/snooze", intent.dataString)
        assertEquals("Planning", intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE))
        assertEquals(4242, intent.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1))
        assertEquals(77L, intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_ID, -1L))
        assertEquals("https://meet.google.com/abc", intent.getStringExtra(AlarmReceiver.EXTRA_MEETING_URL))
    }

    @Test
    fun `snooze defaults to 10 minutes when the preference is missing`() {
        every { mockPrefsRepo.getSnoozeTimeMinutes() } returns emptyFlow()

        createScheduler().scheduleSnooze("T", 1, 1L, 1L)

        assertEquals(fixedClock.millis() + 10 * 60_000L, nextAlarm().triggerAtMs)
    }

    @Test
    fun `snooze keeps the location and end time of the event`() {
        createScheduler().scheduleSnooze("T", 1, 1L, 1L, null, "Room 42", 9_000L)

        val intent = Shadows.shadowOf(nextAlarm().operation).savedIntent
        assertEquals("Room 42", intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_LOCATION))
        assertEquals(9_000L, intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_END_TIME, -1L))
    }

    @Test
    fun `cancel also removes a pending snooze of the event`() {
        val event = Event(id = 10L, title = "T", startTime = 123_456_789L)
        val scheduler = createScheduler()
        scheduler.schedule(event)
        scheduler.scheduleSnooze(event.title, event.uniqueIntentId, event.id, event.startTime)

        scheduler.cancel(event)

        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `cancel keeps snoozes of other events`() {
        val cancelled = Event(id = 1L, title = "A", startTime = 1_000_000L)
        val other = Event(id = 2L, title = "B", startTime = 2_000_000L)
        val scheduler = createScheduler()
        scheduler.scheduleSnooze(other.title, other.uniqueIntentId, other.id, other.startTime)

        scheduler.cancel(cancelled)

        assertEquals(1, Shadows.shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `snooze does not replace the original alarm of the event`() {
        val event = Event(id = 10L, title = "T", startTime = 123_456_789L)
        val scheduler = createScheduler()
        scheduler.schedule(event)

        scheduler.scheduleSnooze(event.title, event.uniqueIntentId, event.id, event.startTime)

        assertEquals(2, Shadows.shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `alarms of events deleted from the calendar are cancelled`() {
        val kept = Event(id = 1L, title = "Kept", startTime = 1_000_000L)
        val deleted = Event(id = 2L, title = "Deleted", startTime = 2_000_000L)
        val scheduler = createScheduler()
        scheduler.schedule(kept)
        scheduler.schedule(deleted)

        scheduler.cancelAlarmsNotIn(listOf(kept))

        assertEquals(listOf(kept.uniqueIntentId), scheduledIds())
    }

    @Test
    fun `moving an event cancels the alarm at its old time`() {
        val before = Event(id = 1L, title = "Meeting", startTime = 1_000_000L)
        val after = before.copy(startTime = 5_000_000L)
        val scheduler = createScheduler()
        scheduler.schedule(before)
        scheduler.schedule(after)

        scheduler.cancelAlarmsNotIn(listOf(after))

        assertEquals(listOf(after.uniqueIntentId), scheduledIds())
    }

    @Test
    fun `a deleted event does not ring after being snoozed either`() {
        val deleted = Event(id = 2L, title = "Deleted", startTime = 2_000_000L)
        val scheduler = createScheduler()
        scheduler.schedule(deleted)
        scheduler.scheduleSnooze(deleted.title, deleted.uniqueIntentId, deleted.id, deleted.startTime)

        scheduler.cancelAlarmsNotIn(emptyList())

        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `events that already started keep their alarm and snooze`() {
        val scheduler = createScheduler()
        val meeting = Event(id = 1L, title = "Meeting", startTime = 1_000_000L)
        scheduler.schedule(meeting)
        scheduler.scheduleSnooze(meeting.title, meeting.uniqueIntentId, meeting.id, meeting.startTime)

        // The meeting started; the calendar window no longer returns it.
        scheduler.clock = Clock.fixed(Instant.ofEpochMilli(1_500_000L), ZoneId.systemDefault())
        scheduler.cancelAlarmsNotIn(emptyList())

        assertEquals(2, Shadows.shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `scheduled alarms are remembered across scheduler instances`() {
        val deleted = Event(id = 2L, title = "Deleted", startTime = 2_000_000L)
        createScheduler().schedule(deleted)

        // The worker usually runs in a new process with a new scheduler instance.
        createScheduler().cancelAlarmsNotIn(emptyList())

        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `alarms cancelled directly are not cancelled again later`() {
        val event = Event(id = 1L, title = "T", startTime = 1_000_000L)
        val scheduler = createScheduler()
        scheduler.schedule(event)
        scheduler.cancel(event)
        scheduler.schedule(event.copy(id = 9L))

        scheduler.cancelAlarmsNotIn(listOf(event.copy(id = 9L)))

        assertEquals(listOf(event.copy(id = 9L).uniqueIntentId), scheduledIds())
    }

    private fun scheduledIds() =
        Shadows.shadowOf(alarmManager).scheduledAlarms.map {
            Shadows.shadowOf(it.operation).savedIntent.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1)
        }

    private fun nextAlarm(): ShadowAlarmManager.ScheduledAlarm {
        val alarm = Shadows.shadowOf(alarmManager).peekNextScheduledAlarm()
        assertNotNull("An alarm should have been scheduled", alarm)
        return alarm!!
    }
}
