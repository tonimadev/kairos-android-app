package digital.tonima.core.ai.model

import digital.tonima.core.viewmodel.UiText

/**
 * Represents the outcome of an AI agent query to the LLM.
 *
 * The LLM can respond with plain text **or** with a function call
 * (tool use) when it decides to execute an action.
 */
sealed class AIAgentResponse {
    /** The LLM returned a textual answer (no tool was invoked). */
    data class Text(val content: String) : AIAgentResponse()

    /** The LLM decided to invoke a registered tool / function. */
    data class FunctionCall(
        val name: String,
        val args: Map<String, Any?>,
    ) : AIAgentResponse()

    /** The LLM returned a genuinely empty (but not failed) response. */
    data object Empty : AIAgentResponse()

    /** The call to the LLM failed; [message] is ready to surface to the user. */
    data class Error(val message: UiText) : AIAgentResponse()
}
