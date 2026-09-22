package digital.tonima.core.ai.usecases

import android.content.Context
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.repository.AiModelRepository
import digital.tonima.core.ai.repository.AiModelResult
import digital.tonima.core.ai.repository.DailyBriefingRepository
import digital.tonima.core.data.repository.WeatherRepository
import digital.tonima.kairos.core.model.Event
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
}
