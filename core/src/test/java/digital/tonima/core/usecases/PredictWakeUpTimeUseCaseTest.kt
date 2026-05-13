package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.CalendarRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PredictWakeUpTimeUseCaseTest {
    private val mockAppPreferencesRepository = mockk<AppPreferencesRepository>()
    private val mockCalendarRepository = mockk<CalendarRepository>()
    private val mockCalculateDepartureTimeUseCase = mockk<CalculateDepartureTimeUseCase>()

    private lateinit var useCase: PredictWakeUpTimeUseCaseImpl

    @Before
    fun setup() {
        useCase =
            PredictWakeUpTimeUseCaseImpl(
                mockAppPreferencesRepository,
                mockCalendarRepository,
                mockCalculateDepartureTimeUseCase,
            )
    }

    @Test
    fun `when no history and no events then return null`() =
        runTest {
            coEvery { mockAppPreferencesRepository.getWakeUpHistory() } returns flowOf(emptyList())
            coEvery { mockCalendarRepository.getEventsForMonth(any()) } returns emptyList()

            val result = useCase()
            assertEquals(null, result)
        }

    @Test
    fun `when has history but no events then return average from history`() =
        runTest {
            val zoneId = ZoneId.systemDefault()
            val time1 = ZonedDateTime.now(zoneId).with(LocalTime.of(7, 0)).toInstant().toEpochMilli()
            val time2 = ZonedDateTime.now(zoneId).with(LocalTime.of(8, 0)).toInstant().toEpochMilli()

            coEvery { mockAppPreferencesRepository.getWakeUpHistory() } returns flowOf(listOf(time1, time2))
            coEvery { mockCalendarRepository.getEventsForMonth(any()) } returns emptyList()

            val result = useCase()
            assertEquals(LocalTime.of(7, 30), result)
        }

    @Test
    fun `when has early event tomorrow then suggest wake up 1h before`() =
        runTest {
            val zoneId = ZoneId.systemDefault()
            // History says 8:00
            val historyTime = ZonedDateTime.now(zoneId).with(LocalTime.of(8, 0)).toInstant().toEpochMilli()
            coEvery { mockAppPreferencesRepository.getWakeUpHistory() } returns flowOf(listOf(historyTime))

            // Tomorrow has an event at 7:00
            val tomorrow = ZonedDateTime.now(zoneId).plusDays(1).with(LocalTime.of(7, 0))
            val event =
                Event(
                    id = 1,
                    title = "Early Meeting",
                    startTime = tomorrow.toInstant().toEpochMilli(),
                )

            coEvery { mockCalendarRepository.getEventsForMonth(any()) } returns listOf(event)
            coEvery { mockCalculateDepartureTimeUseCase(event) } returns null // No departure info

            val result = useCase()
            // Should be 6:00 (1h before 7:00 meeting)
            assertEquals(LocalTime.of(6, 0), result)
        }

    @Test
    fun `when has early departure tomorrow then suggest wake up 1h before departure`() =
        runTest {
            val zoneId = ZoneId.systemDefault()
            // History says 8:00
            val historyTime = ZonedDateTime.now(zoneId).with(LocalTime.of(8, 0)).toInstant().toEpochMilli()
            coEvery { mockAppPreferencesRepository.getWakeUpHistory() } returns flowOf(listOf(historyTime))

            // Tomorrow has an event at 9:00 but departure is 7:30
            val eventTime =
                ZonedDateTime.now(zoneId).plusDays(1).with(LocalTime.of(9, 0))
                    .toInstant().toEpochMilli()
            val departureTime =
                ZonedDateTime.now(zoneId).plusDays(1).with(LocalTime.of(7, 30))
                    .toInstant().toEpochMilli()

            val event = Event(id = 1, title = "Far Meeting", startTime = eventTime)

            coEvery { mockCalendarRepository.getEventsForMonth(any()) } returns listOf(event)
            coEvery { mockCalculateDepartureTimeUseCase(event) } returns
                DepartureInfo(
                    departureTime,
                    90,
                )

            val result = useCase()
            // Should be 6:30 (1h before 7:30 departure)
            assertEquals(LocalTime.of(6, 30), result)
        }
}
