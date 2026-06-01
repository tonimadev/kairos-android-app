package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import javax.inject.Inject

class AnalyzeScheduleTool
    @Inject
    constructor() : AITool {
        override val name: String = "analyze_schedule"

        override val description: String =
            "Analyzes the user's schedule for a given timeframe (e.g., 'month', 'week') " +
                "and provides a summary. Use this when the user asks to organize their time, " +
                "see their monthly metrics, or understand how much time they are spending in meetings."

        override val riskLevel: RiskLevel = RiskLevel.SAFE

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "timeframe" to
                            mapOf(
                                "type" to "string",
                                "description" to "The timeframe to analyze, e.g., 'day', 'week', 'month'",
                            ),
                    ),
                "required" to listOf("timeframe"),
            )

        override fun parseArguments(args: Map<String, Any?>): EventIntent? {
            val timeframe = args["timeframe"] as? String ?: return null

            return EventIntent.AnalyzeSchedule(timeframe)
        }
    }
