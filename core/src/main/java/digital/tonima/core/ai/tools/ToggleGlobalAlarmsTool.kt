package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.BaseIntent
import digital.tonima.core.viewmodel.SettingsIntent
import javax.inject.Inject

/**
 * **MODERATE** tool — toggles the global alarms on or off.
 *
 * Toggling alarms affects every event at once, so it is classified as
 * [RiskLevel.MODERATE]. The ViewModel will dispatch the intent immediately but
 * emit a [digital.tonima.core.viewmodel.EventSideEffect.ShowSnackbar] so the
 * user is aware the AI just changed their alarm settings.
 */
class ToggleGlobalAlarmsTool
    @Inject
    constructor() : AITool {
        override val name: String = "toggle_global_alarms"

        override val description: String =
            "Enables or disables all event alarms globally. " +
                "Use this tool when the user asks to turn on/off all alarms, " +
                "mute all notifications, or enable all reminders."

        override val riskLevel: RiskLevel = RiskLevel.MODERATE

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "enabled" to
                            mapOf(
                                "type" to "boolean",
                                "description" to "true to enable alarms, false to disable",
                            ),
                    ),
                "required" to listOf("enabled"),
            )

        override fun parseArguments(args: Map<String, Any?>): BaseIntent? {
            val enabled = args["enabled"] as? Boolean ?: return null
            return SettingsIntent.ToggleGlobalAlarms(enabled)
        }
    }
