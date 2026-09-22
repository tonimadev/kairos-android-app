package digital.tonima.core.ai.usecases

import android.content.Context
import app.cash.turbine.test
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.repository.AiModelRepository
import digital.tonima.core.ai.repository.AiModelResult
import digital.tonima.core.viewmodel.UiText
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.Event
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AskAiAgentUseCaseImplTest {
    private val context = mockk<Context>(relaxed = true)
    private val aiModelRepository = mockk<AiModelRepository>()
    private val useCase = AskAiAgentUseCaseImpl(context, aiModelRepository)

    private val events =
        listOf(
            Event(id = 1L, title = "Team sync", startTime = 1_710_000_000_000L, isAlarmEnabled = true),
        )

    @Before
    fun setup() {
        every { context.getString(R.string.ai_context_date_prefix) } returns "Date:"
        every { context.getString(R.string.all_day) } returns "All day"
        every { context.getString(R.string.ai_context_alarm_disabled) } returns "(Alarm Disabled)"
        every {
            context.getString(eq(R.string.ai_system_instruction_template), any(), any(), any(), any(), any())
        } answers {
            // The vararg format args arrive as a single Array<Any> element — flatten it so the
            // test can assert on the individual interpolated values without a real resource table.
            val rawArgs = it.invocation.args.drop(1)
            val formatArgs = (rawArgs.singleOrNull() as? Array<*>)?.toList() ?: rawArgs
            formatArgs.joinToString("||")
        }
    }

    @Test
    fun `system instruction passed to the repository contains events and language instruction`() =
        runTest {
            val systemInstructionSlot = slot<String>()
            every {
                aiModelRepository.streamAgentResponse(
                    capture(systemInstructionSlot),
                    any(),
                    any(),
                    any(),
                )
            } returns flowOf(AiModelResult.Text("ok"))

            useCase(events, "Reply in English", "Reply in English", emptySet()).test {
                awaitItem()
                awaitComplete()
            }

            val captured = systemInstructionSlot.captured
            assertTrue(captured.contains("Team sync"))
            assertTrue(captured.contains("Reply in English"))
        }

    @Test
    fun `passes tools, history and question through to the repository unchanged`() =
        runTest {
            val tools = setOf(mockk<AITool>(relaxed = true))
            val history = listOf(ChatMessage.Text(ChatMessage.Role.USER, "hi"))
            every {
                aiModelRepository.streamAgentResponse(any(), any(), any(), any())
            } returns flowOf(AiModelResult.Text("ok"))

            useCase(events, "hello", "Reply in English", tools, history).test {
                awaitItem()
                awaitComplete()
            }

            verify {
                aiModelRepository.streamAgentResponse(any(), tools, history, "hello")
            }
        }

    @Test
    fun `maps AiModelResult Text to AIAgentResponse Text`() =
        runTest {
            every {
                aiModelRepository.streamAgentResponse(any(), any(), any(), any())
            } returns flowOf(AiModelResult.Text("partial"), AiModelResult.Text("partial full"))

            useCase(events, "hi", "Reply in English", emptySet()).test {
                assertEquals(AIAgentResponse.Text("partial"), awaitItem())
                assertEquals(AIAgentResponse.Text("partial full"), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `maps AiModelResult FunctionCall to AIAgentResponse FunctionCall`() =
        runTest {
            every {
                aiModelRepository.streamAgentResponse(any(), any(), any(), any())
            } returns flowOf(AiModelResult.FunctionCall("create_event", mapOf("title" to "Meeting")))

            useCase(events, "create a meeting", "Reply in English", emptySet()).test {
                assertEquals(
                    AIAgentResponse.FunctionCall("create_event", mapOf("title" to "Meeting")),
                    awaitItem(),
                )
                awaitComplete()
            }
        }

    @Test
    fun `maps AiModelResult Error to AIAgentResponse Error with the right message`() =
        runTest {
            every {
                aiModelRepository.streamAgentResponse(any(), any(), any(), any())
            } returns flowOf(AiModelResult.Error(AiErrorType.NETWORK, RuntimeException("boom")))

            useCase(events, "hi", "Reply in English", emptySet()).test {
                val item = awaitItem()
                assertTrue(item is AIAgentResponse.Error)
                assertEquals(
                    UiText.StringResource(R.string.ai_error_network),
                    (item as AIAgentResponse.Error).message,
                )
                awaitComplete()
            }
        }
}
