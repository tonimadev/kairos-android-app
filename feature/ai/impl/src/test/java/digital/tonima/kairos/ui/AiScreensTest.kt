package digital.tonima.kairos.ui

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.entity.ConversationEntity
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.AiSuggestionsDialog
import digital.tonima.kairos.ui.view.ChatDetailScreen
import digital.tonima.kairos.ui.view.ChatHistoryScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp")
class AiScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val calls = mutableListOf<String>()

    // region AiSuggestionsDialog

    @Test
    fun `picking a suggestion sends it and closes the dialog`() {
        suggestionsDialog()

        compose.onNodeWithText(string(R.string.ai_suggestion_agenda)).performClick()

        assertEquals(listOf("suggestion:${string(R.string.ai_suggestion_agenda)}", "dismiss"), calls)
    }

    @Test
    fun `quick actions open the chat or generate the briefing and close the dialog`() {
        suggestionsDialog()

        compose.onNodeWithText(string(R.string.ask_ai_label)).performClick()
        compose.onNodeWithText(string(R.string.ai_briefing)).performClick()

        assertEquals(listOf("chat", "dismiss", "briefing", "dismiss"), calls)
    }

    @Test
    fun `voice capture and cancel are forwarded`() {
        suggestionsDialog()

        compose.onNodeWithText(string(R.string.cd_voice_capture)).performClick()
        compose.onNodeWithText(string(R.string.cancel)).performClick()

        assertEquals(listOf("voice", "dismiss"), calls)
    }

    private fun suggestionsDialog() {
        compose.setContent {
            AiSuggestionsDialog(
                onDismiss = { calls += "dismiss" },
                onSuggestionClick = { calls += "suggestion:$it" },
                onVoiceClick = { calls += "voice" },
                onOpenChat = { calls += "chat" },
                onGenerateBriefing = { calls += "briefing" },
            )
        }
    }

    // endregion

    // region ChatDetailScreen

    @Test
    fun `chat shows the conversation and marks tool activity`() {
        chat(
            messages =
                listOf(
                    ChatMessage.Text(ChatMessage.Role.USER, "Marque dentista amanhã"),
                    ChatMessage.FunctionCall("create_event", mapOf("title" to "Dentista")),
                    ChatMessage.Text(ChatMessage.Role.ASSISTANT, "Pronto, marquei às 10h."),
                ),
        )

        compose.onNodeWithText("Marque dentista amanhã").assertExists()
        compose.onNodeWithText("Pronto, marquei às 10h.").assertExists()
        compose.onNodeWithText("⏳ create_event...").assertExists()
    }

    @Test
    fun `typing shows the send button and sending reports the message`() {
        chat()
        compose.onAllNodesWithContentDescription(string(R.string.cd_send_message)).assertCountEquals(0)

        compose.onNodeWithText(string(R.string.chat_input_placeholder)).performTextInput("Qual minha agenda?")
        compose.onNodeWithContentDescription(string(R.string.cd_send_message)).performClick()

        assertEquals(listOf("send:Qual minha agenda?"), calls)
    }

    @Test
    fun `an empty input offers voice instead of send`() {
        chat()

        compose.onNodeWithContentDescription(string(R.string.cd_voice_capture)).performClick()

        assertEquals(listOf("speak"), calls)
    }

    @Test
    fun `streaming answer is shown while the assistant is replying`() {
        chat(isAsking = true, streamingText = "Você tem 3 reu")

        compose.onNodeWithText("Você tem 3 reu").assertExists()
    }

    @Test
    fun `close returns from the chat`() {
        chat()

        compose.onNodeWithContentDescription(string(R.string.cd_close)).performClick()

        assertEquals(listOf("back"), calls)
    }

    private fun chat(
        messages: List<ChatMessage> = emptyList(),
        isAsking: Boolean = false,
        streamingText: String? = null,
    ) {
        compose.setContent {
            ChatDetailScreen(
                messages = messages,
                isAsking = isAsking,
                isSpeaking = false,
                onBack = { calls += "back" },
                onSendMessage = { calls += "send:$it" },
                onSpeakToggle = { calls += "speak" },
                streamingText = streamingText,
            )
        }
    }

    // endregion

    // region ChatHistoryScreen

    @Test
    fun `chat history lists conversations and opens or deletes them`() {
        chatHistory(listOf(ConversationEntity(id = 3L, title = "Agenda da semana", createdAt = 1L, updatedAt = 1L)))

        compose.onNodeWithText("Agenda da semana").performClick()
        compose.onNodeWithContentDescription(string(R.string.cd_delete_conversation)).performClick()

        assertEquals(listOf("open:3", "delete:3"), calls)
    }

    @Test
    fun `empty chat history invites starting a conversation`() {
        chatHistory(emptyList())

        compose.onNodeWithText(string(R.string.no_conversations_yet)).assertExists()
        compose.onNodeWithContentDescription(string(R.string.new_conversation_title)).performClick()
        compose.onNodeWithContentDescription(string(R.string.cd_close)).performClick()

        assertEquals(listOf("new:${string(R.string.new_conversation_title)}", "back"), calls)
    }

    private fun chatHistory(conversations: List<ConversationEntity>) {
        compose.setContent {
            ChatHistoryScreen(
                conversations = conversations,
                onBack = { calls += "back" },
                onConversationClick = { calls += "open:$it" },
                onCreateNewChat = { calls += "new:$it" },
                onDeleteConversation = { calls += "delete:$it" },
            )
        }
    }

    // endregion

    private fun string(resId: Int) = app.getString(resId)
}
