package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

interface PredictWakeUpTimeUseCase {
    suspend operator fun invoke(): LocalTime?
}

@BindType(installIn = BindType.Component.SINGLETON, to = PredictWakeUpTimeUseCase::class)
class PredictWakeUpTimeUseCaseImpl
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
    ) : PredictWakeUpTimeUseCase {
        override suspend fun invoke(): LocalTime? {
            val history = repository.getWakeUpHistory().first()
            if (history.isEmpty()) return null

            val zoneId = ZoneId.systemDefault()

            // Extrair apenas o horário (LocalTime) de cada timestamp
            val times =
                history.map {
                    Instant.ofEpochMilli(it).atZone(zoneId).toLocalTime()
                }

            // Se houver apenas 1 registro, usamos ele
            if (times.size == 1) return times[0]

            // Calcular a média dos horários em segundos desde o início do dia
            val averageSeconds = times.map { it.toSecondOfDay() }.average().toInt()

            return LocalTime.ofSecondOfDay(averageSeconds.toLong())
        }
    }
