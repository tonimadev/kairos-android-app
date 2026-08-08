package digital.tonima.core.ai

import digital.tonima.core.viewmodel.BaseIntent

/**
 * Represents the result of processing an AI tool call in the [ActionRegistry].
 */
sealed class AIToolResult {
    /**
     * The tool was found and its arguments were successfully parsed into an [EventIntent].
     */
    data class Success(
        val tool: AITool,
        val intent: BaseIntent,
    ) : AIToolResult()

    /**
     * The tool name was not found in the registry.
     */
    data class ToolNotFound(val toolName: String) : AIToolResult()

    /**
     * The tool was found but the arguments could not be parsed into a valid intent.
     */
    data class InvalidArguments(
        val toolName: String,
        val args: Map<String, Any?>,
    ) : AIToolResult()
}
