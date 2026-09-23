package digital.tonima.core.service

import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import digital.tonima.core.data.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.data.usecases.DepartureInfo
import digital.tonima.core.data.usecases.GetEventsForMonthUseCase
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.permissions.PermissionManager
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.repository.RingerModeRepository
import digital.tonima.kairos.core.model.Event
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class AlarmSchedulingWorkerDoWorkTest {
    private val getEventsForMonth: GetEventsForMonthUseCase = mockk()
    private val scheduler: EventAlarmScheduler = mockk(relaxed = true)
    private val permissionManager: PermissionManager = mockk()
    private val preferences: AppPreferencesRepository = mockk()
    private val proUserProvider: ProUserProvider = mockk()
    private val calculateDepartureTime: CalculateDepartureTimeUseCase = mockk()
    private val ringerModeRepository: RingerModeRepository = mockk()

    @Before
    fun setUp() {
        every { permissionManager.hasCalendarPermission() } returns true
        every { preferences.isGlobalAlarmEnabled() } returns flowOf(true)
        every { preferences.getAlarmOffsetMinutes() } returns flowOf(0L)
        every { preferences.isAllDayAlarmsEnabled() } returns flowOf(true)
        every { preferences.getAllDayAlarmHour() } returns flowOf(9)
        every { preferences.getDisabledEventIds() } returns flowOf(emptySet())
        every { preferences.getDisabledSeriesIds() } returns flowOf(emptySet())
        every { proUserProvider.isAiUser } returns MutableStateFlow(false)
        every { ringerModeRepository.ringerMode } returns MutableStateFlow(AudioWarningState.NORMAL)
    }

    @Test
    fun `schedules only events whose alarm fires within the next 7 days`() =
        runTest {
            val now = System.currentTimeMillis()
            val past = timedEvent(1, now - hours(1))
            val tomorrow = timedEvent(2, now + hours(24))
            val inSixDays = timedEvent(3, now + TimeUnit.DAYS.toMillis(6))
            val inEightDays = timedEvent(4, now + TimeUnit.DAYS.toMillis(8))
            givenEvents(past, tomorrow, inSixDays, inEightDays)

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            verify(exactly = 1) { scheduler.schedule(tomorrow, null) }
            verify(exactly = 1) { scheduler.schedule(inSixDays, null) }
            verify(exactly = 0) { scheduler.schedule(past, any()) }
            verify(exactly = 0) { scheduler.schedule(inEightDays, any()) }
        }

    @Test
    fun `events from the current and the next month are both considered`() =
        runTest {
            val now = System.currentTimeMillis()
            val thisMonth = timedEvent(1, now + hours(2))
            val nextMonth = timedEvent(2, now + hours(3))
            coEvery { getEventsForMonth(any()) } returnsMany listOf(listOf(thisMonth), listOf(nextMonth))

            worker().doWork()

            verify { scheduler.schedule(thisMonth, null) }
            verify { scheduler.schedule(nextMonth, null) }
        }

    @Test
    fun `alarm offset skips events whose alarm time already passed`() =
        runTest {
            every { preferences.getAlarmOffsetMinutes() } returns flowOf(15L)
            val now = System.currentTimeMillis()
            val startsInTenMinutes = timedEvent(1, now + minutes(10))
            val startsInTwentyMinutes = timedEvent(2, now + minutes(20))
            givenEvents(startsInTenMinutes, startsInTwentyMinutes)

            worker().doWork()

            verify(exactly = 0) { scheduler.schedule(startsInTenMinutes, any()) }
            verify(exactly = 1) { scheduler.schedule(startsInTwentyMinutes, null) }
        }

    @Test
    fun `alarm offset extends the window for events just after 7 days`() =
        runTest {
            every { preferences.getAlarmOffsetMinutes() } returns flowOf(30L)
            val now = System.currentTimeMillis()
            // Alarm fires 30 min before start, i.e. still inside the 7-day window.
            val justAfterWindow = timedEvent(1, now + TimeUnit.DAYS.toMillis(7) + minutes(10))
            givenEvents(justAfterWindow)

            worker().doWork()

            verify(exactly = 1) { scheduler.schedule(justAfterWindow, null) }
        }

    @Test
    fun `disabled occurrences and disabled series are not scheduled`() =
        runTest {
            val now = System.currentTimeMillis()
            val disabledOccurrence = timedEvent(1, now + hours(1))
            val disabledSeries = timedEvent(2, now + hours(2))
            val enabled = timedEvent(3, now + hours(3))
            every { preferences.getDisabledEventIds() } returns
                flowOf(setOf(disabledOccurrence.uniqueIntentId.toString()))
            every { preferences.getDisabledSeriesIds() } returns flowOf(setOf(disabledSeries.id.toString()))
            givenEvents(disabledOccurrence, disabledSeries, enabled)

            worker().doWork()

            verify(exactly = 0) { scheduler.schedule(disabledOccurrence, any()) }
            verify(exactly = 0) { scheduler.schedule(disabledSeries, any()) }
            verify(exactly = 1) { scheduler.schedule(enabled, null) }
        }

    @Test
    fun `nothing is scheduled when alarms are globally disabled`() =
        runTest {
            every { preferences.isGlobalAlarmEnabled() } returns flowOf(false)
            givenEvents(timedEvent(1, System.currentTimeMillis() + hours(1)))

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            verify(exactly = 0) { scheduler.schedule(any(), any()) }
            coVerify(exactly = 0) { getEventsForMonth(any()) }
        }

    @Test
    fun `nothing is scheduled without calendar permission`() =
        runTest {
            every { permissionManager.hasCalendarPermission() } returns false

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            verify(exactly = 0) { scheduler.schedule(any(), any()) }
            coVerify(exactly = 0) { getEventsForMonth(any()) }
        }

    @Test
    fun `missing preferences fall back to alarms enabled with no offset`() =
        runTest {
            every { preferences.isGlobalAlarmEnabled() } returns emptyFlow()
            every { preferences.getAlarmOffsetMinutes() } returns emptyFlow()
            every { preferences.getDisabledEventIds() } returns emptyFlow()
            every { preferences.getDisabledSeriesIds() } returns emptyFlow()
            val event = timedEvent(1, System.currentTimeMillis() + minutes(5))
            givenEvents(event)

            worker().doWork()

            verify(exactly = 1) { scheduler.schedule(event, null) }
        }

    @Test
    fun `all-day events are scheduled at the configured hour when enabled`() =
        runTest {
            val allDay = allDayEvent(1, LocalDate.now(ZoneOffset.UTC).plusDays(2))
            givenEvents(allDay)

            worker().doWork()

            verify(exactly = 1) { scheduler.schedule(allDay, null) }
        }

    @Test
    fun `all-day events are skipped when all-day alarms are disabled`() =
        runTest {
            every { preferences.isAllDayAlarmsEnabled() } returns flowOf(false)
            val allDay = allDayEvent(1, LocalDate.now(ZoneOffset.UTC).plusDays(2))
            givenEvents(allDay)

            worker().doWork()

            verify(exactly = 0) { scheduler.schedule(any(), any()) }
        }

    @Test
    fun `all-day events whose alarm hour already passed today are skipped`() =
        runTest {
            val today = LocalDate.now(ZoneId.systemDefault())
            // Alarm hour set to 00:00 local, which is always in the past by the time the worker runs.
            every { preferences.getAllDayAlarmHour() } returns flowOf(0)
            val allDayToday =
                allDayEvent(1, today).also {
                    val fireTime = today.atTime(LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant()
                    check(fireTime.toEpochMilli() <= System.currentTimeMillis())
                }
            givenEvents(allDayToday)

            worker().doWork()

            verify(exactly = 0) { scheduler.schedule(any(), any()) }
        }

    @Test
    fun `ai users with a located event get a traffic-aware trigger time`() =
        runTest {
            every { proUserProvider.isAiUser } returns MutableStateFlow(true)
            val now = System.currentTimeMillis()
            val located = timedEvent(1, now + hours(3)).copy(location = "Av. Paulista, 1000")
            val remote = timedEvent(2, now + hours(4))
            val departure = located.startTime - minutes(40)
            coEvery { calculateDepartureTime(located) } returns DepartureInfo(departure, 25)
            givenEvents(located, remote)

            worker().doWork()

            verify(exactly = 1) { scheduler.schedule(located, departure) }
            verify(exactly = 1) { scheduler.schedule(remote, null) }
            coVerify(exactly = 0) { calculateDepartureTime(remote) }
        }

    @Test
    fun `ai users fall back to the regular alarm when departure time is unknown`() =
        runTest {
            every { proUserProvider.isAiUser } returns MutableStateFlow(true)
            val located = timedEvent(1, System.currentTimeMillis() + hours(3)).copy(location = "Somewhere")
            coEvery { calculateDepartureTime(located) } returns null
            givenEvents(located)

            worker().doWork()

            verify(exactly = 1) { scheduler.schedule(located, null) }
        }

    @Test
    fun `non ai users never query traffic`() =
        runTest {
            val located = timedEvent(1, System.currentTimeMillis() + hours(3)).copy(location = "Somewhere")
            givenEvents(located)

            worker().doWork()

            verify(exactly = 1) { scheduler.schedule(located, null) }
            coVerify(exactly = 0) { calculateDepartureTime(any()) }
        }

    @Test
    fun `alarms of events no longer in the calendar are cancelled on every run`() =
        runTest {
            val now = System.currentTimeMillis()
            val thisMonth = timedEvent(1, now + hours(2))
            val nextMonth = timedEvent(2, now + TimeUnit.DAYS.toMillis(20))
            coEvery { getEventsForMonth(any()) } returnsMany listOf(listOf(thisMonth), listOf(nextMonth))

            worker().doWork()

            verify { scheduler.cancelAlarmsNotIn(listOf(thisMonth, nextMonth)) }
        }

    @Test
    fun `worker reports failure when loading events throws`() =
        runTest {
            coEvery { getEventsForMonth(any()) } throws SecurityException("calendar provider unavailable")

            assertEquals(ListenableWorker.Result.failure(), worker().doWork())
        }

    private fun worker() =
        AlarmSchedulingWorker(
            ApplicationProvider.getApplicationContext(),
            mockk<WorkerParameters>(relaxed = true),
            getEventsForMonth,
            scheduler,
            permissionManager,
            preferences,
            proUserProvider,
            calculateDepartureTime,
            ringerModeRepository,
        )

    private fun givenEvents(vararg events: Event) {
        coEvery { getEventsForMonth(any()) } returnsMany listOf(events.toList(), emptyList())
    }

    private fun timedEvent(
        id: Long,
        startTime: Long,
    ) = Event(id = id, title = "Event $id", startTime = startTime, endTime = startTime + hours(1))

    /** All-day events are stored by the calendar provider at midnight UTC of their date. */
    private fun allDayEvent(
        id: Long,
        date: LocalDate,
    ) = Event(
        id = id,
        title = "All-day $id",
        startTime = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        isAllDay = true,
    )

    private fun hours(value: Long) = TimeUnit.HOURS.toMillis(value)

    private fun minutes(value: Long) = TimeUnit.MINUTES.toMillis(value)
}
