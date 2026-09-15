package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import javax.inject.Inject

/**
 * **CRITICAL** tool — creates a new event on the user's calendar.
 *
 * Because creating an event is irreversible from the AI's perspective, it is
 * classified as [RiskLevel.CRITICAL]. The ViewModel will **pause** execution,
 * store the generated [EventIntent.CreateEvent], and emit a
 * [digital.tonima.core.viewmodel.EventSideEffect.RequireUserConfirmation] so
 * the UI can ask the user to confirm before persisting the event.
 */
class CreateEventTool
    @Inject
    constructor() : AITool {
        override val name: String = "create_event"

        override val description: String =
            "Creates a new event on the user's calendar. " +
                "Use this tool when the user asks to schedule, create, or add a meeting, " +
                "appointment, or reminder. Requires at least a title and start/end timestamps " +
                "(epoch millis). calendarId should be the ID of the calendar to add the event to; " +
                "if unknown, use 1 as default."

        override val riskLevel: RiskLevel = RiskLevel.CRITICAL

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "calendar_id" to
                            mapOf(
                                "type" to "number",
                                "description" to "Calendar ID to add the event to (default: 1)",
                            ),
                        "title" to
                            mapOf(
                                "type" to "string",
                                "description" to "Event title",
                            ),
                        "description" to
                            mapOf(
                                "type" to "string",
                                "description" to "Optional event description / notes",
                            ),
                        "location" to
                            mapOf(
                                "type" to "string",
                                "description" to "Optional event location",
                            ),
                        "start_time" to
                            mapOf(
                                "type" to "number",
                                "description" to "Start time as epoch milliseconds",
                            ),
                        "end_time" to
                            mapOf(
                                "type" to "number",
                                "description" to "End time as epoch milliseconds",
                            ),
                        "is_all_day" to
                            mapOf(
                                "type" to "boolean",
                                "description" to "Whether this is an all-day event (default: false)",
                            ),
                    ),
                "required" to listOf("title", "start_time", "end_time"),
            )

        override fun parseArguments(args: Map<String, Any?>): EventIntent? {
            val title = args["title"]?.toString()?.takeIf { it.isNotBlank() }
            val startTime = (args["start_time"] as? Number)?.toLong()
            val endTime = (args["end_time"] as? Number)?.toLong()

            if (title == null || startTime == null || endTime == null || endTime <= startTime) {
                return null
            }

            return EventIntent.CreateEvent(
                calendarId = (args["calendar_id"] as? Number)?.toLong() ?: 1L,
                title = title,
                description = args["description"]?.toString(),
                location = args["location"]?.toString(),
                startTime = startTime,
                endTime = endTime,
                isAllDay = args["is_all_day"] as? Boolean ?: false,
            )
        }
    }
