package digital.tonima.core.usecases

import digital.tonima.core.ai.AIToolResult
import digital.tonima.core.ai.ActionRegistry
import javax.inject.Inject

class ProcessAiResponseUseCase
    @Inject
    constructor(
        private val actionRegistry: ActionRegistry,
    ) {
        operator fun invoke(
            toolName: String,
            args: Map<String, Any?>,
        ): AIToolResult {
            return actionRegistry.processAIToolCall(toolName, args)
        }
    }
