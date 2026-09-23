package digital.tonima.core.ai.appfunctions

import android.content.Context
import androidx.appfunctions.AppFunctionContext
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.usecases.BriefingResult
import digital.tonima.core.ai.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.data.usecases.CreateEventUseCase
import digital.tonima.core.data.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.data.usecases.GetEventsForMonthUseCase
import digital.tonima.core.viewmodel.UiText
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.DeviceCalendar
import digital.tonima.kairos.core.model.Event
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class KairosAppFunctionsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val createEventUseCase: CreateEventUseCase = mockk()
    private val generateDailyBriefingUseCase: GenerateDailyBriefingUseCase = mockk()
    private val getEventsForMonthUseCase: GetEventsForMonthUseCase = mockk()
    private val getAvailableCalendarsUseCase: GetAvailableCalendarsUseCase = mockk()
    private val appFunctionContext: AppFunctionContext = mockk()
    private val functions =
        KairosAppFunctions(
            context,
            createEventUseCase,
            generateDailyBriefingUseCase,
            getEventsForMonthUseCase,
            getAvailableCalendarsUseCase,
        )

    @Before
    fun setUp() {
        coEvery { getAvailableCalendarsUseCase() } returns
            listOf(
                DeviceCalendar(id = 1L, displayName = "Pessoal", accountName = "me"),
                DeviceCalendar(id = 3L, displayName = "Trabalho", accountName = "me"),
            )
    }

    @Test
    fun `an event without an end time lasts one hour`() =
        runTest {
            coEvery { createEventUseCase(1L, "Reunião", null, null, 1_000L, 3_601_000L, false) } returns 55L

            assertEquals(55L, functions.createEvent(appFunctionContext, "Reunião", 1_000L))
        }

    @Test
    fun `a failed event creation returns -1`() =
        runTest {
            coEvery { createEventUseCase(any(), any(), any(), any(), any(), any(), any()) } returns null

            val id =
                functions.createEvent(
                    appFunctionContext,
                    title = "Feriado",
                    startTime = 1_000L,
                    endTime = 2_000L,
                    calendarId = 3L,
                    description = "desc",
                    location = "loc",
                    isAllDay = true,
                )

            assertEquals(-1L, id)
            coVerify { createEventUseCase(3L, "Feriado", "desc", "loc", 1_000L, 2_000L, true) }
        }

    @Test
    fun `an unknown calendar falls back to the first available one`() =
        runTest {
            coEvery { getAvailableCalendarsUseCase() } returns
                listOf(DeviceCalendar(id = 7L, displayName = "Único", accountName = "me"))
            coEvery { createEventUseCase(7L, any(), any(), any(), any(), any(), any()) } returns 9L

            assertEquals(9L, functions.createEvent(appFunctionContext, "Reunião", 1_000L, calendarId = 1L))
        }

    @Test
    fun `without any writable calendar no event is created`() =
        runTest {
            coEvery { getAvailableCalendarsUseCase() } returns emptyList()

            assertEquals(-1L, functions.createEvent(appFunctionContext, "Reunião", 1_000L))
            coVerify(exactly = 0) { createEventUseCase(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `the daily briefing uses today's events from the enabled calendars in the device language`() =
        runTest {
            val noon = LocalDate.now().atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val today = Event(id = 1L, title = "Hoje", startTime = noon)
            val otherDay = Event(id = 2L, title = "Outro", startTime = noon - 5 * 86_400_000L)
            coEvery { getEventsForMonthUseCase(YearMonth.now().atDay(1).toEpochDay()) } returns listOf(today, otherDay)
            val deviceLanguage = context.getString(R.string.ai_briefing_instruction)
            coEvery { generateDailyBriefingUseCase(listOf(today), deviceLanguage, null, null) } returns
                BriefingResult.Success("Good morning")

            assertEquals("Good morning", functions.getDailyBriefing(appFunctionContext))
        }

    @Test
    fun `a cached briefing is returned as is and errors use the translated message`() =
        runTest {
            coEvery { getEventsForMonthUseCase(any()) } returns emptyList()

            coEvery { generateDailyBriefingUseCase(any(), any(), any(), any()) } returns BriefingResult.Cached("Cache")
            assertEquals("Cache", functions.getDailyBriefing(appFunctionContext))

            coEvery { generateDailyBriefingUseCase(any(), any(), any(), any()) } returns
                BriefingResult.Error(UiText.StringResource(R.string.ai_error_network), AiErrorType.NETWORK)
            assertEquals(context.getString(R.string.ai_error_network), functions.getDailyBriefing(appFunctionContext))
        }
}
