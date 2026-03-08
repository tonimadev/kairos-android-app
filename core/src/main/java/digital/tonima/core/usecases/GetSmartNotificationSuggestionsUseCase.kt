package digital.tonima.core.usecases

import digital.tonima.core.model.Event

interface GetSmartNotificationSuggestionsUseCase {
    suspend fun invoke(
        recentEvents: List<Event>,
        languageInstruction: String,
    ): String?
}
