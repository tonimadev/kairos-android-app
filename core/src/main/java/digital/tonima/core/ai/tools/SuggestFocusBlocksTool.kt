package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.AiIntent
import digital.tonima.core.viewmodel.BaseIntent
import javax.inject.Inject

class SuggestFocusBlocksTool
    @Inject
    constructor() : AITool {
        override val name: String = "suggest_focus_blocks"

        override val description: String =
            "Suggests and creates focus blocks in the calendar during gaps between events. " +
                "Helps the user reserve time for deep work."

        override val riskLevel: RiskLevel = RiskLevel.CRITICAL

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "start_time" to
                            mapOf(
                                "type" to "number",
                                "description" to "Start time of the focus block as epoch milliseconds",
                            ),
                        "end_time" to
                            mapOf(
                                "type" to "number",
                                "description" to "End time of the focus block as epoch milliseconds",
                            ),
                    ),
                "required" to listOf("start_time", "end_time"),
            )

        override fun parseArguments(args: Map<String, Any?>): BaseIntent? {
            val startTime = (args["start_time"] as? Number)?.toLong() ?: return null
            val endTime = (args["end_time"] as? Number)?.toLong() ?: return null

            return AiIntent.CreateFocusBlock(
                startTime = startTime,
                endTime = endTime,
            )
        }
    }
