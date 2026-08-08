package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.AiIntent
import digital.tonima.core.viewmodel.BaseIntent
import javax.inject.Inject

class RescheduleEventTool
    @Inject
    constructor() : AITool {
        override val name: String = "reschedule_event"

        override val description: String =
            "Reschedules an existing event to a new time. " +
                "Use this to resolve conflicts or when the user asks to move an appointment."

        override val riskLevel: RiskLevel = RiskLevel.CRITICAL

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "event_id" to
                            mapOf(
                                "type" to "string",
                                "description" to "The unique ID of the event to reschedule",
                            ),
                        "new_start_time" to
                            mapOf(
                                "type" to "number",
                                "description" to "New start time as epoch milliseconds",
                            ),
                        "new_end_time" to
                            mapOf(
                                "type" to "number",
                                "description" to "New end time as epoch milliseconds",
                            ),
                    ),
                "required" to listOf("event_id", "new_start_time", "new_end_time"),
            )

        override fun parseArguments(args: Map<String, Any?>): BaseIntent? {
            val eventId = args["event_id"]?.toString()
            val startTime = (args["new_start_time"] as? Number)?.toLong()
            val endTime = (args["new_end_time"] as? Number)?.toLong()

            return if (eventId != null && startTime != null && endTime != null) {
                AiIntent.RescheduleEvent(
                    eventId = eventId,
                    newStartTime = startTime,
                    newEndTime = endTime,
                )
            } else {
                null
            }
        }
    }
