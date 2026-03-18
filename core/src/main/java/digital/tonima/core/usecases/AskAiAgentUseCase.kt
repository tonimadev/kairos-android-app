package digital.tonima.core.usecases

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.model.Event

/**
 * Sends a question to the LLM **with function-calling support**.
 *
 * When the model decides to invoke a tool, it returns
 * [AIAgentResponse.FunctionCall]; otherwise it returns [AIAgentResponse.Text].
 */
interface AskAiAgentUseCase {
    suspend operator fun invoke(
        events: List<Event>,
        question: String,
        languageInstruction: String,
        availableTools: Set<AITool>,
    ): AIAgentResponse
}
