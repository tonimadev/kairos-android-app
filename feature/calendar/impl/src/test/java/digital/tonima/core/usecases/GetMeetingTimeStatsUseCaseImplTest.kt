package digital.tonima.core.usecases

import android.content.Context
import com.google.common.collect.ImmutableList
import digital.tonima.core.data.repository.CalendarRepository
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.Event
import digital.tonima.kairos.core.model.InsightsPeriod
import io.mockk.coEvery
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
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class GetMeetingTimeStatsUseCaseImplTest {
    private lateinit var mockCalendarRepository: CalendarRepository
    private lateinit var mockAppPreferencesRepository: AppPreferencesRepository
    private lateinit var mockContext: Context
    private lateinit var useCase: GetMeetingTimeStatsUseCaseImpl

    private val zone = ZoneId.systemDefault()

    @Before
    fun setup() {
        mockCalendarRepository = mockk()
        mockAppPreferencesRepository = mockk()
        mockContext = mockk()
        every { mockAppPreferencesRepository.getEnabledCalendarIds() } returns flowOf(emptySet())
        every { mockContext.getString(R.string.insights_morning) } returns "Morning"
        every { mockContext.getString(R.string.insights_afternoon) } returns "Afternoon"
        every { mockContext.getString(R.string.insights_evening) } returns "Evening"
        every { mockContext.getString(R.string.insights_week_format, any()) } answers {
            "W${secondArg<Int>()}"
        }
        useCase = GetMeetingTimeStatsUseCaseImpl(mockCalendarRepository, mockAppPreferencesRepository, mockContext)
    }

    private fun toEpochMillis(
        date: LocalDate,
        time: LocalTime = LocalTime.of(10, 0),
    ): Long = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()

    @Test
    fun `week stats include meeting from the previous month when the week starts there`() =
        runTest {
            // Pick a "now" that is the first day of a month, so Monday of that week
            // necessarily falls in the previous month.
            val now = LocalDate.of(2024, 5, 1) // Wednesday May 1st 2024
            val fixedClock = Clock.fixed(now.atStartOfDay(zone).toInstant(), zone)
            useCase.clock = fixedClock

            val mondayOfWeek = now.minusDays(now.dayOfWeek.value.toLong() - 1) // April 29th 2024
            check(mondayOfWeek.month != now.month) { "test premise requires week to span two months" }

            val eventInPreviousMonth =
                Event(
                    id = 1L,
                    title = "Prev month meeting",
                    startTime = toEpochMillis(mondayOfWeek),
                    endTime = toEpochMillis(mondayOfWeek, LocalTime.of(11, 0)),
                    meetingUrl = "https://meet.example.com/a",
                )

            coEvery {
                mockCalendarRepository.getEventsForMonth(
                    YearMonth.from(now).atDay(1).toEpochDay(),
                    ImmutableList.of(),
                )
            } returns ImmutableList.of()

            coEvery {
                mockCalendarRepository.getEventsForMonth(
                    YearMonth.from(mondayOfWeek).atDay(1).toEpochDay(),
                    ImmutableList.of(),
                )
            } returns ImmutableList.of(eventInPreviousMonth)

            val result = useCase(InsightsPeriod.WEEK)

            val mondayLabel = mondayOfWeek.format(DateTimeFormatter.ofPattern("EEE"))
            val mondayHours = result.first { it.first == mondayLabel }.second
            assertEquals(1f, mondayHours, 0.001f)
        }

    @Test
    fun `day stats bucket events into morning, afternoon and evening`() =
        runTest {
            val now = LocalDate.of(2024, 6, 10)
            val fixedClock = Clock.fixed(now.atStartOfDay(zone).toInstant(), zone)
            useCase.clock = fixedClock

            val morningEvent =
                Event(
                    id = 1L,
                    title = "Morning meeting",
                    startTime = toEpochMillis(now, LocalTime.of(9, 0)),
                    endTime = toEpochMillis(now, LocalTime.of(9, 30)),
                    meetingUrl = "https://meet.example.com/a",
                )
            val eveningEvent =
                Event(
                    id = 2L,
                    title = "Evening meeting",
                    startTime = toEpochMillis(now, LocalTime.of(19, 0)),
                    endTime = toEpochMillis(now, LocalTime.of(20, 0)),
                    meetingUrl = "https://meet.example.com/b",
                )

            coEvery {
                mockCalendarRepository.getEventsForMonth(
                    YearMonth.from(now).atDay(1).toEpochDay(),
                    ImmutableList.of(),
                )
            } returns ImmutableList.of(morningEvent, eveningEvent)

            val result = useCase(InsightsPeriod.DAY)

            assertEquals(0.5f, result.first { it.first == "Morning" }.second, 0.001f)
            assertEquals(0f, result.first { it.first == "Afternoon" }.second, 0.001f)
            assertEquals(1f, result.first { it.first == "Evening" }.second, 0.001f)
        }

    @Test
    fun `events without a meeting url are excluded from stats`() =
        runTest {
            val now = LocalDate.of(2024, 6, 10)
            val fixedClock = Clock.fixed(now.atStartOfDay(zone).toInstant(), zone)
            useCase.clock = fixedClock

            val nonMeetingEvent =
                Event(
                    id = 1L,
                    title = "Focus block",
                    startTime = toEpochMillis(now, LocalTime.of(9, 0)),
                    endTime = toEpochMillis(now, LocalTime.of(10, 0)),
                    meetingUrl = null,
                )

            coEvery {
                mockCalendarRepository.getEventsForMonth(
                    YearMonth.from(now).atDay(1).toEpochDay(),
                    ImmutableList.of(),
                )
            } returns ImmutableList.of(nonMeetingEvent)

            val result = useCase(InsightsPeriod.DAY)

            assertEquals(0f, result.sumOf { it.second.toDouble() }.toFloat(), 0.001f)
        }
}
