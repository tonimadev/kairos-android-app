package digital.tonima.core.usecases

import digital.tonima.core.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignOutFromGoogleUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke() = authRepository.signOut()
    }
