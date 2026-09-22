package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.kairos.core.model.Event
import kotlinx.coroutines.flow.Flow

/**
 * Streams a question to the LLM **with function-calling support**.
 *
 * Each [AIAgentResponse.Text] emission carries the cumulative answer so far; the terminal
 * emission is either the final [AIAgentResponse.Text], an [AIAgentResponse.FunctionCall] when
 * the model decides to invoke a tool, or an [AIAgentResponse.Error].
 */
interface AskAiAgentUseCase {
    operator fun invoke(
        events: List<Event>,
        question: String?, // Nullable for when we just want to resume chat after a function response
        languageInstruction: String,
        availableTools: Set<AITool>,
        history: List<ChatMessage> = emptyList(),
    ): Flow<AIAgentResponse>
}
