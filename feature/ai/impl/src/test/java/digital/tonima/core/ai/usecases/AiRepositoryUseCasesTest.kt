package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.AIToolResult
import digital.tonima.core.ai.ActionRegistry
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.repository.ChatHistoryRepository
import digital.tonima.core.ai.repository.DailyBriefingRepository
import digital.tonima.core.database.entity.ConversationEntity
import digital.tonima.core.utils.TextToSpeechHelper
import digital.tonima.core.utils.WidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The chat, tool and briefing use cases are thin adapters: they must hand data through untouched. */
class AiRepositoryUseCasesTest {
    private val chatRepository: ChatHistoryRepository = mockk(relaxed = true)
    private val actionRegistry: ActionRegistry = mockk()
    private val message = ChatMessage.Text(ChatMessage.Role.USER, "oi")

    @Test
    fun `conversation use cases delegate to the chat repository`() =
        runTest {
            val conversations = listOf(mockk<ConversationEntity>())
            coEvery { chatRepository.createConversation("Nova") } returns 4L
            coEvery { chatRepository.getHistory(4L) } returns listOf(message)
            coEvery { chatRepository.insertMessage(4L, message) } returns 9L
            coEvery { chatRepository.clearHistory(4L) } returns 1
            every { chatRepository.observeHistory(4L) } returns flowOf(listOf(message))
            every { chatRepository.observeConversations() } returns flowOf(conversations)

            assertEquals(4L, CreateConversationUseCase(chatRepository)("Nova"))
            assertEquals(listOf(message), GetChatHistoryUseCase(chatRepository)(4L))
            assertEquals(9L, InsertChatMessageUseCase(chatRepository)(4L, message))
            assertEquals(1, ClearChatHistoryUseCase(chatRepository)(4L))
            assertEquals(listOf(message), ObserveChatHistoryUseCase(chatRepository)(4L).first())
            assertEquals(conversations, ObserveConversationsUseCase(chatRepository)().first())
            DeleteConversationUseCase(chatRepository)(4L)

            coVerify { chatRepository.deleteConversation(4L) }
        }

    @Test
    fun `tool use cases delegate to the action registry`() {
        val tool = mockk<AITool>()
        val result = AIToolResult.ToolNotFound("x")
        every { actionRegistry.registeredTools() } returns setOf(tool)
        every { actionRegistry.processAIToolCall("x", mapOf("a" to 1)) } returns result

        assertEquals(setOf(tool), GetRegisteredAiToolsUseCase(actionRegistry)())
        assertEquals(result, ProcessAiResponseUseCase(actionRegistry)("x", mapOf("a" to 1)))
    }

    @Test
    fun `daily briefing is observed from its repository`() =
        runTest {
            val repository = mockk<DailyBriefingRepository> { every { getDailyBriefing() } returns flowOf("Bom dia") }

            assertEquals("Bom dia", ObserveDailyBriefingUseCase(repository)().first())
        }

    @Test
    fun `speech and widget use cases delegate to their helpers`() =
        runTest {
            val tts = mockk<TextToSpeechHelper>(relaxed = true)
            val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
            val onDone = {}

            SpeakTextUseCase(tts)("Olá", onDone)
            SpeakTextUseCase(tts).stop()
            UpdateWidgetUseCase(widgetUpdater).updateDailyBriefingWidget()

            verify { tts.speak("Olá", onDone) }
            verify { tts.stop() }
            coVerify { widgetUpdater.updateDailyBriefingWidget() }
        }
}
