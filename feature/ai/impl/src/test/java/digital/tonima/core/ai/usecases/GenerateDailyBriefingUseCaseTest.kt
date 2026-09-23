package digital.tonima.core.ai.usecases

import android.content.Context
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.repository.AiModelRepository
import digital.tonima.core.ai.repository.AiModelResult
import digital.tonima.core.ai.repository.DailyBriefingRepository
import digital.tonima.core.data.repository.WeatherRepository
import digital.tonima.core.viewmodel.UiText
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.Event
import digital.tonima.kairos.core.model.Weather
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GenerateDailyBriefingUseCaseTest {
    private val context = mockk<Context>(relaxed = true)
    private val weatherRepository = mockk<WeatherRepository>()
    private val dailyBriefingRepository = mockk<DailyBriefingRepository>(relaxed = true)
    private val aiModelRepository = mockk<AiModelRepository>()
    private val useCase =
        GenerateDailyBriefingUseCaseImpl(context, weatherRepository, dailyBriefingRepository, aiModelRepository)

    private val events =
        listOf(
            Event(id = 1L, title = "Event 1", startTime = 1710000000000L, isAllDay = false),
        )

    @Before
    fun setup() {
        coEvery { dailyBriefingRepository.getLastGeneratedDate() } returns null
    }

    @Test
    fun `when already generated today should return cached without calling the model`() =
        runBlocking {
            coEvery { dailyBriefingRepository.getLastGeneratedDate() } returns LocalDate.now()
            every { dailyBriefingRepository.getDailyBriefing() } returns flowOf("Cached briefing")

            val result = useCase.invoke(events, "Instruction", null)

            assertEquals(BriefingResult.Cached("Cached briefing"), result)
            coVerify(exactly = 0) { aiModelRepository.generateBriefingContent(any()) }
        }

    @Test
    fun `when not yet generated today should call the model and cache the result`() =
        runBlocking {
            coEvery { aiModelRepository.generateBriefingContent(any()) } returns AiModelResult.Text("Briefing content")

            val result = useCase.invoke(events, "Instruction", "08:00")

            assertEquals(BriefingResult.Success("Briefing content"), result)
            coVerify { dailyBriefingRepository.saveDailyBriefing("Briefing content", LocalDate.now()) }
        }

    @Test
    fun `when the model call fails should return a typed error`() =
        runBlocking {
            val cause = RuntimeException("boom")
            coEvery { aiModelRepository.generateBriefingContent(any()) } returns
                AiModelResult.Error(AiErrorType.NETWORK, cause)

            val result = useCase.invoke(events, "Instruction", null)

            assertTrue(result is BriefingResult.Error)
            assertEquals(AiErrorType.NETWORK, (result as BriefingResult.Error).errorType)
        }

    @Test
    fun `every model error type maps to its own message`() =
        runBlocking {
            val expected =
                mapOf(
                    AiErrorType.NETWORK to R.string.ai_error_network,
                    AiErrorType.RATE_LIMITED to R.string.ai_error_rate_limited,
                    AiErrorType.SAFETY_BLOCKED to R.string.ai_error_safety_blocked,
                    AiErrorType.UNKNOWN to R.string.ai_error_unknown,
                )
            expected.forEach { (type, message) ->
                coEvery { aiModelRepository.generateBriefingContent(any()) } returns
                    AiModelResult.Error(type, RuntimeException("model failed"))

                val result = useCase.invoke(events, "Instruction", null) as BriefingResult.Error

                assertEquals(UiText.StringResource(message), result.message)
            }
        }

    @Test
    fun `an unexpected tool call from the model is an unknown error`() =
        runBlocking {
            coEvery { aiModelRepository.generateBriefingContent(any()) } returns
                AiModelResult.FunctionCall("search", emptyMap())

            val result = useCase.invoke(events, "Instruction", null) as BriefingResult.Error

            assertEquals(AiErrorType.UNKNOWN, result.errorType)
        }

    @Test
    fun `a stale cache from an earlier day is regenerated`() =
        runBlocking {
            coEvery { dailyBriefingRepository.getLastGeneratedDate() } returns LocalDate.now()
            every { dailyBriefingRepository.getDailyBriefing() } returns flowOf(null)
            coEvery { aiModelRepository.generateBriefingContent(any()) } returns AiModelResult.Text("Novo")

            assertEquals(BriefingResult.Success("Novo"), useCase.invoke(events, "Instruction", null))
        }

    @Test
    fun `the prompt includes the weather, all-day events and an empty day`() =
        runBlocking {
            val weather =
                Weather(temperature = 21.7, description = "nublado", icon = "04d", city = "Recife", conditionCode = 803)
            coEvery { weatherRepository.getWeather("Recife", any(), any()) } returns weather
            every { context.getString(R.string.all_day) } returns "Dia todo"
            every { context.getString(R.string.ai_context_no_events_today) } returns "Sem eventos"
            coEvery { aiModelRepository.generateBriefingContent(any()) } returns AiModelResult.Text("ok")

            val holiday = Event(id = 2L, title = "Feriado", startTime = 0L, isAllDay = true)
            useCase.invoke(listOf(holiday), "pt", null, "Recife")
            useCase.invoke(emptyList(), "pt", null, null)

            verify { context.getString(R.string.ai_context_weather, "Recife", 21, "nublado") }
            verify { context.getString(R.string.all_day) }
            verify { context.getString(R.string.ai_context_no_events_today) }
        }
}
