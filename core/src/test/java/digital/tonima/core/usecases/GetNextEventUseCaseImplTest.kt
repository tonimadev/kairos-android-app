package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.CalendarRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class GetNextEventUseCaseImplTest {
    private lateinit var mockEventsRepository: CalendarRepository
    private lateinit var mockAppPreferencesRepository: AppPreferencesRepository
    private lateinit var getNextEventUseCase: GetNextEventUseCase

    @Before
    fun setup() {
        mockEventsRepository = mockk()
        mockAppPreferencesRepository = mockk()
        every { mockAppPreferencesRepository.getEnabledCalendarIds() } returns flowOf(emptySet())
        getNextEventUseCase = GetNextEventUseCaseImpl(mockEventsRepository, mockAppPreferencesRepository)
    }

    @Test
    fun `invoke calls repository and returns list of events`() =
        runTest {
            fun toEpochMillis(
                date: LocalDate,
                time: LocalTime,
            ): Long {
                return ZonedDateTime.of(date, time, ZoneId.systemDefault()).toInstant().toEpochMilli()
            }

            val expectedEvent =
                Event(
                    id = 1L,
                    title = "Test Event 1",
                    startTime = toEpochMillis(LocalDate.of(2023, 10, 26), LocalTime.of(10, 0)),
                    isAlarmEnabled = true,
                )
            coEvery { mockEventsRepository.getNextUpcomingEvent(emptyList()) } returns expectedEvent

            val result = getNextEventUseCase.invoke()

            assertEquals(expectedEvent, result)
            coVerify(exactly = 1) { mockEventsRepository.getNextUpcomingEvent(emptyList()) }
        }
}
