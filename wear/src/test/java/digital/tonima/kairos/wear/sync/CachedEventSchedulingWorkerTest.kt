package digital.tonima.kairos.wear.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.kairos.core.model.Event
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = android.app.Application::class)
class CachedEventSchedulingWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences: AppPreferencesRepository = mockk()
    private val scheduler: EventAlarmScheduler = mockk(relaxed = true)

    @Before
    fun setUp() {
        context.getSharedPreferences("PhoneEventsCache", Context.MODE_PRIVATE).edit().clear().commit()
        every { preferences.isGlobalAlarmEnabled() } returns flowOf(true)
        every { preferences.getAlarmOffsetMinutes() } returns flowOf(0L)
        every { preferences.isAllDayAlarmsEnabled() } returns flowOf(true)
        every { preferences.getAllDayAlarmHour() } returns flowOf(9)
        every { preferences.getDisabledEventIds() } returns flowOf(emptySet())
        every { preferences.getDisabledSeriesIds() } returns flowOf(emptySet())
    }

    @Test
    fun `schedules cached events whose alarm fires within the next 24 hours`() =
        runTest {
            val now = System.currentTimeMillis()
            val past = event(1, now - hours(1))
            val soon = event(2, now + hours(2))
            val tomorrowLate = event(3, now + hours(30))
            WearEventCache.save(context, listOf(past, soon, tomorrowLate))

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            verify(exactly = 1) { scheduler.schedule(soon, null) }
            verify(exactly = 0) { scheduler.schedule(past, any()) }
            verify(exactly = 0) { scheduler.schedule(tomorrowLate, any()) }
        }

    @Test
    fun `nothing is scheduled when alarms are globally disabled`() =
        runTest {
            every { preferences.isGlobalAlarmEnabled() } returns flowOf(false)
            WearEventCache.save(context, listOf(event(1, System.currentTimeMillis() + hours(1))))

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            verify(exactly = 0) { scheduler.schedule(any(), any()) }
        }

    @Test
    fun `disabled occurrences and series are not scheduled on the watch`() =
        runTest {
            val now = System.currentTimeMillis()
            val disabledOccurrence = event(1, now + hours(1))
            val disabledSeries = event(2, now + hours(2))
            val enabled = event(3, now + hours(3))
            every { preferences.getDisabledEventIds() } returns
                flowOf(setOf(disabledOccurrence.uniqueIntentId.toString()))
            every { preferences.getDisabledSeriesIds() } returns flowOf(setOf(disabledSeries.id.toString()))
            WearEventCache.save(context, listOf(disabledOccurrence, disabledSeries, enabled))

            worker().doWork()

            verify(exactly = 0) { scheduler.schedule(disabledOccurrence, any()) }
            verify(exactly = 0) { scheduler.schedule(disabledSeries, any()) }
            verify(exactly = 1) { scheduler.schedule(enabled, null) }
        }

    @Test
    fun `alarm offset skips events whose alarm time already passed`() =
        runTest {
            every { preferences.getAlarmOffsetMinutes() } returns flowOf(15L)
            val now = System.currentTimeMillis()
            val startsInTenMinutes = event(1, now + TimeUnit.MINUTES.toMillis(10))
            val startsInOneHour = event(2, now + hours(1))
            WearEventCache.save(context, listOf(startsInTenMinutes, startsInOneHour))

            worker().doWork()

            verify(exactly = 0) { scheduler.schedule(startsInTenMinutes, any()) }
            verify(exactly = 1) { scheduler.schedule(startsInOneHour, null) }
        }

    @Test
    fun `all-day events are skipped when all-day alarms are disabled`() =
        runTest {
            every { preferences.isAllDayAlarmsEnabled() } returns flowOf(false)
            // The all-day switch must win regardless of whether the alarm hour falls in the window.
            val allDay =
                Event(
                    id = 1L,
                    title = "Holiday",
                    startTime =
                        LocalDate.now(
                            ZoneOffset.UTC,
                        ).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    isAllDay = true,
                )
            WearEventCache.save(context, listOf(allDay))

            worker().doWork()

            verify(exactly = 0) { scheduler.schedule(any(), any()) }
        }

    @Test
    fun `empty cache succeeds without scheduling anything`() =
        runTest {
            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            verify(exactly = 0) { scheduler.schedule(any(), any()) }
        }

    @Test
    fun `worker reports failure when preferences cannot be read`() =
        runTest {
            every { preferences.isGlobalAlarmEnabled() } throws IllegalStateException("datastore corrupted")

            assertEquals(ListenableWorker.Result.failure(), worker().doWork())
        }

    private fun worker() =
        CachedEventSchedulingWorker(
            context,
            mockk<WorkerParameters>(relaxed = true),
            preferences,
            scheduler,
        )

    private fun event(
        id: Long,
        startTime: Long,
    ) = Event(id = id, title = "Event $id", startTime = startTime)

    private fun hours(value: Long) = TimeUnit.HOURS.toMillis(value)
}
