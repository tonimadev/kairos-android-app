package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import javax.inject.Inject

/**
 * Example **SAFE** tool — searches events on the user's calendar.
 *
 * Because it is [RiskLevel.SAFE] the ViewModel will dispatch the generated
 * intent immediately with no extra confirmation or snackbar.
 */
class SearchTool
    @Inject
    constructor() : AITool {
        override val name: String = "search_events"

        override val description: String =
            "Searches events on the user's calendar by keyword. " +
                "Use this tool when the user asks to find, look up, or search for events."

        override val riskLevel: RiskLevel = RiskLevel.SAFE

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "query" to
                            mapOf(
                                "type" to "string",
                                "description" to "Search term to filter events",
                            ),
                    ),
                "required" to listOf("query"),
            )

        override fun parseArguments(args: Map<String, Any?>): EventIntent? {
            val query = args["query"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
            return EventIntent.SearchQueryChanged(query)
        }
    }
