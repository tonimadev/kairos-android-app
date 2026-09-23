package digital.tonima.core.database.mapper

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.entity.ChatHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHistoryMapperTest {
    @Test
    fun `text messages round trip for both roles`() {
        listOf(ChatMessage.Role.USER, ChatMessage.Role.ASSISTANT).forEach { role ->
            val message = ChatMessage.Text(role, "Qual minha próxima reunião? 🙂")

            assertEquals(message, message.toEntity(conversationId = 3L).toChatMessage())
        }
    }

    @Test
    fun `entities keep the conversation they belong to`() {
        val entity = ChatMessage.Text(ChatMessage.Role.USER, "hi").toEntity(conversationId = 42L)

        assertEquals(42L, entity.conversationId)
        assertEquals("USER", entity.role)
        assertEquals("TEXT", entity.type)
    }

    @Test
    fun `function calls round trip with string arguments`() {
        val call =
            ChatMessage.FunctionCall(
                "create_event",
                mapOf("title" to "Dentista", "location" to "Rua Augusta, 500"),
            )

        val restored = call.toEntity(conversationId = 1L).toChatMessage()

        assertEquals(call, restored)
    }

    @Test
    fun `function call numbers and booleans come back as their string form`() {
        val call = ChatMessage.FunctionCall("toggle_alarms", mapOf("enabled" to true, "minutes" to 15))

        val restored = call.toEntity(conversationId = 1L).toChatMessage() as ChatMessage.FunctionCall

        assertEquals(mapOf("enabled" to "true", "minutes" to "15"), restored.args)
    }

    @Test
    fun `function responses round trip and are attributed to the user role`() {
        val response = ChatMessage.FunctionResponse("search_events", mapOf("result" to "2 events found"))

        val entity = response.toEntity(conversationId = 1L)

        assertEquals("USER", entity.role)
        assertEquals("FUNCTION_RESPONSE", entity.type)
        assertEquals(response, entity.toChatMessage())
    }

    @Test
    fun `function calls are attributed to the assistant role`() {
        val entity = ChatMessage.FunctionCall("x", emptyMap()).toEntity(conversationId = 1L)

        assertEquals("ASSISTANT", entity.role)
        assertEquals("FUNCTION_CALL", entity.type)
    }

    @Test
    fun `corrupted function arguments load as empty instead of crashing`() {
        val entity = entity(type = "FUNCTION_CALL", functionName = "create_event", payload = "{not json")

        val restored = entity.toChatMessage() as ChatMessage.FunctionCall

        assertEquals("create_event", restored.name)
        assertTrue(restored.args.isEmpty())
    }

    @Test
    fun `missing function payload loads as empty`() {
        val restored = entity(type = "FUNCTION_RESPONSE", functionName = "search", payload = null).toChatMessage()

        assertEquals(ChatMessage.FunctionResponse("search", emptyMap()), restored)
    }

    @Test
    fun `rows that cannot be displayed are skipped`() {
        assertNull(entity(type = "FUNCTION_CALL", functionName = null, payload = "{}").toChatMessage())
        assertNull(entity(type = "FUNCTION_RESPONSE", functionName = null, payload = "{}").toChatMessage())
        assertNull(entity(type = "TEXT", content = null).toChatMessage())
        assertNull(entity(type = "IMAGE", content = "x").toChatMessage())
    }

    private fun entity(
        type: String,
        content: String? = null,
        functionName: String? = null,
        payload: String? = null,
    ) = ChatHistoryEntity(
        conversationId = 1L,
        role = "ASSISTANT",
        type = type,
        content = content,
        functionName = functionName,
        functionArgsOrResponse = payload,
    )
}
