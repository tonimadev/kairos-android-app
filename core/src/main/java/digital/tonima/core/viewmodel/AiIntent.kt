package digital.tonima.core.viewmodel

sealed class AiIntent : BaseIntent {
    data object ConsumeEffect : AiIntent()

    data class AskAi(val question: String, val language: String) : AiIntent()

    data class GenerateDailyBriefing(val language: String) : AiIntent()

    object SpeakAiResponse : AiIntent()

    object StopSpeaking : AiIntent()

    object ClearAiResponse : AiIntent()

    object OpenChatHistoryScreen : AiIntent()

    object CloseChatHistoryScreen : AiIntent()

    data class OpenChatDetail(val conversationId: Long) : AiIntent()

    object CloseChatDetail : AiIntent()

    data class CreateNewChat(val title: String) : AiIntent()

    data class DeleteChat(val conversationId: Long) : AiIntent()

    object ApprovePendingAction : AiIntent()

    object RejectPendingAction : AiIntent()

    data class NotifyRunningLate(val eventId: String, val message: String) : AiIntent()

    data class ToggleFocusMode(val enabled: Boolean) : AiIntent()

    data class CreateFocusBlock(
        val startTime: Long,
        val endTime: Long,
        val title: String = "Foco (AI Sugestão)",
    ) : AiIntent()

    data class SummarizeMeetTranscript(val meetingUrl: String) : AiIntent()

    object ShowAiSuggestionsDialog : AiIntent()

    object DismissAiSuggestionsDialog : AiIntent()

    // Missing tools-related intents
    data class AnalyzeSchedule(val timeframe: String) : AiIntent()

    data class CategorizeEvent(val eventId: String, val category: String) : AiIntent()

    data class RescheduleEvent(
        val eventId: String,
        val newStartTime: Long,
        val newEndTime: Long,
    ) : AiIntent()
}
