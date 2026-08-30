package digital.tonima.core.ai.model

import androidx.compose.runtime.Immutable
import com.google.common.collect.ImmutableMap

@Immutable
sealed interface ChatMessage {
    val role: Role

    enum class Role { USER, ASSISTANT }

    data class Text(override val role: Role, val content: String) : ChatMessage

    data class FunctionCall(
        val name: String,
        val args: ImmutableMap<String, Any?>,
    ) : ChatMessage {
        override val role = Role.ASSISTANT
    }

    data class FunctionResponse(
        val name: String,
        val response: ImmutableMap<String, Any?>,
    ) : ChatMessage {
        override val role = Role.USER
    }
}
