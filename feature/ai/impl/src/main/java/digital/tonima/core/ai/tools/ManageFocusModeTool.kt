package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import javax.inject.Inject

/**
 * **MODERATE** tool — manages "Do Not Disturb" (Focus Mode).
 *
 * Allows the AI to enable or disable DND based on user request or meeting density.
 */
class ManageFocusModeTool
    @Inject
    constructor() : AITool {
        override val name: String = "manage_focus_mode"

        override val description: String =
            "Enables or disables 'Do Not Disturb' (Focus Mode). " +
                "Use this tool when the user asks to silence their phone, focus, " +
                "or when the AI suggests protecting focus time."

        override val riskLevel: RiskLevel = RiskLevel.MODERATE

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "enabled" to
                            mapOf(
                                "type" to "boolean",
                                "description" to "Whether to enable (true) or disable (false) DND",
                            ),
                    ),
                "required" to listOf("enabled"),
            )

        override fun parseArguments(args: Map<String, Any?>): EventIntent? {
            val enabled = args["enabled"] as? Boolean ?: return null
            return EventIntent.ToggleFocusMode(enabled)
        }
    }
