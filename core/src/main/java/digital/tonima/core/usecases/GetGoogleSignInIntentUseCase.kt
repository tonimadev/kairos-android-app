package digital.tonima.core.usecases

import digital.tonima.core.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetGoogleSignInIntentUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        operator fun invoke() = authRepository.getSignInIntent()
    }
