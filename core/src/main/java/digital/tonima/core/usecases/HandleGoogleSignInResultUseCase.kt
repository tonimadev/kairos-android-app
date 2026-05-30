package digital.tonima.core.usecases

import android.content.Intent
import digital.tonima.core.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandleGoogleSignInResultUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(intent: Intent?) = authRepository.handleSignInResult(intent)
    }
