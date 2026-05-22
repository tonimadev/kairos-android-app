package digital.tonima.core.ai.model

sealed interface ChatMessage {
    val role: Role

    enum class Role { USER, ASSISTANT }

    data class Text(override val role: Role, val content: String) : ChatMessage

    data class FunctionCall(
        val name: String,
        val args: Map<String, Any?>,
    ) : ChatMessage {
        override val role = Role.ASSISTANT
    }

    data class FunctionResponse(
        val name: String,
        val response: Map<String, Any?>,
    ) : ChatMessage {
        override val role = Role.USER
    }
}
