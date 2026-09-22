package digital.tonima.core.ai.usecases

import digital.tonima.kairos.core.model.Event

interface GenerateDailyBriefingUseCase {
    suspend operator fun invoke(
        events: List<Event>,
        languageInstruction: String,
        wakeUpTime: String? = null,
        city: String? = null,
    ): BriefingResult
}
