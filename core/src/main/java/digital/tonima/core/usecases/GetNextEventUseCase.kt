package digital.tonima.core.usecases

import digital.tonima.kairos.core.model.Event

interface GetNextEventUseCase {
    suspend operator fun invoke(): Event?
}
