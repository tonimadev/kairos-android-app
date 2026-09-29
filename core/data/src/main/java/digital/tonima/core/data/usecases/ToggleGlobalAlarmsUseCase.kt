package digital.tonima.core.data.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Flips the global alarms switch and returns the new state. */
interface ToggleGlobalAlarmsUseCase {
    suspend operator fun invoke(): Boolean
}

@BindType(installIn = BindType.Component.SINGLETON, to = ToggleGlobalAlarmsUseCase::class)
@Singleton
class ToggleGlobalAlarmsUseCaseImpl
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
    ) : ToggleGlobalAlarmsUseCase {
        override suspend fun invoke(): Boolean {
            val enabled = !repository.isGlobalAlarmEnabled().first()
            repository.setGlobalAlarmEnabled(enabled)
            return enabled
        }
    }
