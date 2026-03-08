package digital.tonima.core.usecases

import digital.tonima.core.model.Event

interface GenerateDailyBriefingUseCase {
    suspend fun invoke(
        events: List<Event>,
        languageInstruction: String,
        wakeUpTime: String? = null,
    ): String?
}
