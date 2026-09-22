package digital.tonima.core.ai.repository

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Boundary between the AI use cases and the underlying LLM SDK (Firebase AI Logic / Gemini).
 *
 * Use cases talk only in domain types ([AITool], [ChatMessage], plain prompt strings) so the
 * SDK, retry policy, rate limiting and error classification stay contained to the impl.
 */
interface AiModelRepository {
    /**
     * Streams the agent's response to [question] (or, when [question] is null, resumes the
     * conversation after a function response already appended to [history]).
     *
     * Each emitted [AiModelResult.Text] carries the *cumulative* text produced so far.
     */
    fun streamAgentResponse(
        systemInstruction: String,
        availableTools: Set<AITool>,
        history: List<ChatMessage>,
        question: String?,
    ): Flow<AiModelResult>

    /** One-shot, non-streamed generation used for the daily briefing. */
    suspend fun generateBriefingContent(prompt: String): AiModelResult
}
