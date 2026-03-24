package digital.tonima.core.ai.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: Role,
    val content: String,
) {
    enum class Role {
        USER,
        ASSISTANT,
    }
}
