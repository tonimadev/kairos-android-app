package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.repository.FocusModeRepository
import javax.inject.Inject
import javax.inject.Singleton

interface ToggleFocusModeUseCase {
    operator fun invoke(enabled: Boolean): Result<Unit>

    fun hasAccess(): Boolean
}

@BindType(installIn = BindType.Component.SINGLETON, to = ToggleFocusModeUseCase::class)
@Singleton
class ToggleFocusModeUseCaseImpl
    @Inject
    constructor(
        private val focusModeRepository: FocusModeRepository,
    ) : ToggleFocusModeUseCase {
        override fun invoke(enabled: Boolean): Result<Unit> {
            return if (focusModeRepository.hasNotificationPolicyAccess()) {
                focusModeRepository.setDoNotDisturb(enabled)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Permission for DND access required"))
            }
        }

        override fun hasAccess(): Boolean {
            return focusModeRepository.hasNotificationPolicyAccess()
        }
    }
