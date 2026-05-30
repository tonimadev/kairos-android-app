package digital.tonima.core.usecases

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.ActionRegistry
import javax.inject.Inject

class GetRegisteredAiToolsUseCase
    @Inject
    constructor(
        private val actionRegistry: ActionRegistry,
    ) {
        operator fun invoke(): Set<AITool> {
            return actionRegistry.registeredTools()
        }
    }
