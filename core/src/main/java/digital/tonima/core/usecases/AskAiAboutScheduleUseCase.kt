package digital.tonima.core.usecases

import digital.tonima.core.model.Event

interface AskAiAboutScheduleUseCase {
    suspend operator fun invoke(
        events: List<Event>,
        question: String,
        languageInstruction: String,
    ): String?
}
