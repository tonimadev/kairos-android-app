package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import javax.inject.Inject

/**
 * **MODERATE** tool — suggests sending a "running late" message to meeting participants.
 *
 * Because it involves external communication (even if suggested), it is
 * classified as [RiskLevel.MODERATE]. The ViewModel will dispatch the intent
 * and show a snackbar or notification to the user.
 */
class NotifyLateTool
    @Inject
    constructor() : AITool {
        override val name: String = "notify_late"

        override val description: String =
            "Suggests sending a 'running late' notification to meeting participants. " +
                "Use this tool when the AI detects the user will be late or when the user " +
                "explicitly asks to let others know they are delayed."

        override val riskLevel: RiskLevel = RiskLevel.MODERATE

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "event_id" to
                            mapOf(
                                "type" to "string",
                                "description" to "The unique ID of the event",
                            ),
                        "message" to
                            mapOf(
                                "type" to "string",
                                "description" to "The message to be sent " +
                                    "(e.g., 'Stuck in traffic, be there in 10 mins')",
                            ),
                    ),
                "required" to listOf("event_id", "message"),
            )

        override fun parseArguments(args: Map<String, Any?>): EventIntent? {
            val eventId = args["event_id"]?.toString() ?: return null
            val message = args["message"]?.toString() ?: return null

            return EventIntent.NotifyRunningLate(eventId, message)
        }
    }
