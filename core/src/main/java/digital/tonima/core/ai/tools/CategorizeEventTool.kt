package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.AiIntent.CategorizeEvent
import digital.tonima.core.viewmodel.BaseIntent
import javax.inject.Inject

class CategorizeEventTool
    @Inject
    constructor() : AITool {
        override val name: String = "categorize_event"

        override val description: String =
            "Assigns a category (e.g., Work, Health, Personal) to an event. " +
                "Helpful for organization and time tracking."

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
                        "category" to
                            mapOf(
                                "type" to "string",
                                "description" to "The category name to assign",
                            ),
                    ),
                "required" to listOf("event_id", "category"),
            )

        override fun parseArguments(args: Map<String, Any?>): BaseIntent? {
            val eventId = args["event_id"]?.toString() ?: return null
            val category = args["category"]?.toString() ?: return null

            return CategorizeEvent(
                eventId = eventId,
                category = category,
            )
        }
    }
