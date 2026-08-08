package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.AiIntent
import digital.tonima.core.viewmodel.BaseIntent
import javax.inject.Inject

/**
 * **SAFE** tool — used to fetch and summarize the transcription of a Google Meet meeting.
 *
 * It uses the GoogleMeetRepository to fetch the transcript, and if successful, we can dispatch
 * an intent to pass the transcript back to the AI for summarization, or directly into the chat context.
 *
 * For now, we will map it to a new Intent `SummarizeMeetTranscript(meetingUrl: String)` which
 * the ViewModel will handle by fetching the transcript and passing it back to the agent.
 */
class SummarizeMeetTool
    @Inject
    constructor() : AITool {
        override val name: String = "summarize_meet"

        override val description: String =
            "Fetches the transcript of a past Google Meet meeting and summarizes it. " +
                "Use this tool when the user asks to summarize, transcribe, or retrieve notes from a meeting. " +
                "Requires the meeting URL or code."

        override val riskLevel: RiskLevel = RiskLevel.SAFE

        override val parametersSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "meeting_url" to
                            mapOf(
                                "type" to "string",
                                "description" to "The Google Meet URL or meeting code (e.g. 'abc-defg-hij')",
                            ),
                    ),
                "required" to listOf("meeting_url"),
            )

        override fun parseArguments(args: Map<String, Any?>): BaseIntent? {
            val meetingUrl = args["meeting_url"]?.toString()?.takeIf { it.isNotBlank() } ?: return null

            return AiIntent.SummarizeMeetTranscript(meetingUrl)
        }
    }
