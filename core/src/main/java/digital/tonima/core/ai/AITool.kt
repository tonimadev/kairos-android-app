package digital.tonima.core.ai

import digital.tonima.core.viewmodel.BaseIntent

/**
 * Contract for an AI-callable tool (function calling / tool use).
 *
 * Each implementation maps an LLM function call to a concrete [BaseIntent]
 * that the MVI layer can process as if the user had triggered it directly.
 *
 * @see ActionRegistry
 */
interface AITool {
    /** Unique function name sent to / received from the LLM. */
    val name: String

    /** Natural-language description included in the LLM system prompt so the model
     *  knows *when* and *how* to invoke this tool. */
    val description: String

    /** Determines how the app handles execution (auto, snackbar, or confirmation). */
    val riskLevel: RiskLevel

    /**
     * JSON schema of the parameters this tool expects.
     * Sent to the LLM so it can generate valid arguments.
     *
     * Example:
     * ```json
     * {
     *   "type": "object",
     *   "properties": {
     *     "query": { "type": "string", "description": "Search term" }
     *   },
     *   "required": ["query"]
     * }
     * ```
     */
    val parametersSchema: Map<String, Any>

    /**
     * Attempts to convert the raw argument map returned by the LLM into a valid
     * [BaseIntent].
     *
     * @param args Key-value pairs parsed from the LLM JSON response.
     * @return A valid [BaseIntent] or `null` when the arguments are invalid
     *         (graceful degradation — the caller should log and inform the user).
     */
    fun parseArguments(args: Map<String, Any?>): BaseIntent?
}
