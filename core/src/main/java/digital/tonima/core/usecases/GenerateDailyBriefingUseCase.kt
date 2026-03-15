package digital.tonima.core.usecases

import digital.tonima.core.model.Event

interface GenerateDailyBriefingUseCase {
    suspend operator fun invoke(
        events: List<Event>,
        languageInstruction: String,
        wakeUpTime: String? = null,
        city: String? = null,
    ): String?
}
