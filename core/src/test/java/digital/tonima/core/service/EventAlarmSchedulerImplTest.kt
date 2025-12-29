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

        // Default: all-day alarms disabled
        coEvery { mockPrefsRepo.isAllDayAlarmsEnabled() } returns flowOf(false)
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
        val allDayEvent = Event(id = 10L, title = "All Day Event", startTime = 123456789L, isAllDay = true)

        val scheduler = EventAlarmSchedulerImpl(context, mockPrefsRepo)
        scheduler.schedule(allDayEvent)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assert(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `schedule should set alarm for all-day events when enabled`() {
        // Enable all-day alarms
        coEvery { mockPrefsRepo.isAllDayAlarmsEnabled() } returns flowOf(true)
        coEvery { mockPrefsRepo.getAllDayAlarmHour() } returns flowOf(9)

        val allDayEvent = Event(id = 10L, title = "All Day Event", startTime = 123456789L, isAllDay = true)

        val scheduler = EventAlarmSchedulerImpl(context, mockPrefsRepo)
        scheduler.schedule(allDayEvent)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val next = shadowAlarmManager.nextScheduledAlarm
        assert(next != null)
        assert(next!!.type == AlarmManager.RTC_WAKEUP)
        // O alarme deve ser agendado, mas não no timestamp original (que é UTC)
        assert(next.triggerAtTime != allDayEvent.startTime)
    }
}
