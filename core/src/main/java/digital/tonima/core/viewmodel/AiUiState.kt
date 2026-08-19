package digital.tonima.core.viewmodel

import androidx.compose.runtime.Immutable
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.entity.ConversationEntity

@Immutable
data class AiUiState(
    val dailyBriefing: String? = null,
    val isGeneratingBriefing: Boolean = false,
    val aiResponse: String? = null,
    val isAskingAi: Boolean = false,
    val lastAiQuestion: String? = null,
    val isSpeaking: Boolean = false,
    val chatHistory: List<ChatMessage> = emptyList(),
    val conversations: List<ConversationEntity> = emptyList(),
    val showChatHistoryScreen: Boolean = false,
    val selectedConversationId: Long? = null,
    val voiceEventData: VoiceEventData? = null,
    val showAiSuggestionsDialog: Boolean = false,
    val pendingAIAction: BaseIntent? = null,
    val sideEffects: List<AiSideEffect> = emptyList(),
)
