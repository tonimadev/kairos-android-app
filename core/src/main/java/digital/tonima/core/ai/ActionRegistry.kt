package digital.tonima.core.ai

import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central registry that holds every [AITool] the agent can invoke.
 *
 * Responsibilities:
 * 1. **Discovery** — exposes tool metadata so the LLM knows which functions are available.
 * 2. **Dispatch** — given a tool name + raw args, finds the matching tool, parses
 *    the arguments, and returns an [AIToolResult].
 *
 * Injected via Hilt as a singleton; new tools are added simply by providing them
 * into the `Set<AITool>` multibinding.
 */
@Singleton
class ActionRegistry
    @Inject
    constructor(
        private val tools: Set<@JvmSuppressWildcards AITool>,
    ) {
        private val toolsByName: Map<String, AITool> by lazy {
            tools.associateBy { it.name }
        }

        /** Returns all registered tools (used to build Gemini function declarations). */
        fun registeredTools(): Set<@JvmSuppressWildcards AITool> = tools

        // -----------------------------------------------------------------
        // Discovery — send this to the LLM as the available function list.
        // -----------------------------------------------------------------

        /**
         * Returns a list of tool descriptors ready to be serialized and sent to the LLM
         * as the `tools` / `functions` parameter.
         *
         * Each entry follows the common function-calling schema:
         * ```json
         * {
         *   "name": "transfer_money",
         *   "description": "Transfers money between accounts…",
         *   "parameters": { … }
         * }
         * ```
         */
        fun availableToolDescriptors(): List<Map<String, Any>> =
            tools.map { tool ->
                mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to tool.parametersSchema,
                )
            }

        // -----------------------------------------------------------------
        // Dispatch — called when the LLM response includes a function call.
        // -----------------------------------------------------------------

        /**
         * Looks up [toolName] in the registry, delegates argument parsing to the
         * matching [AITool], and wraps the outcome in an [AIToolResult].
         */
        fun processAIToolCall(
            toolName: String,
            args: Map<String, Any?>,
        ): AIToolResult {
            val tool =
                toolsByName[toolName]
                    ?: run {
                        logcat { "ActionRegistry: tool '$toolName' not found. Available: ${toolsByName.keys}" }
                        return AIToolResult.ToolNotFound(toolName)
                    }

            val intent =
                tool.parseArguments(args)
                    ?: run {
                        logcat { "ActionRegistry: failed to parse args for '$toolName': $args" }
                        return AIToolResult.InvalidArguments(toolName, args)
                    }

            logcat { "ActionRegistry: resolved '$toolName' → ${intent::class.simpleName}" }
            return AIToolResult.Success(tool, intent)
        }
    }
