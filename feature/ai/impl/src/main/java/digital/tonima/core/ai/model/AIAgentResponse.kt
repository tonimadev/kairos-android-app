package digital.tonima.core.ai.model

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

    /** The LLM call failed or returned an empty response. */
    data object Empty : AIAgentResponse()
}
