package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.CalendarRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth
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
        private val calendarRepository: CalendarRepository,
        private val calculateDepartureTimeUseCase: CalculateDepartureTimeUseCase,
    ) : PredictWakeUpTimeUseCase {
        override suspend fun invoke(): LocalTime? {
            // Get base average from history
            val history = repository.getWakeUpHistory().first()
            val zoneId = ZoneId.systemDefault()

            var predictedTime: LocalTime? = null

            if (history.isNotEmpty()) {
                val times = history.map { Instant.ofEpochMilli(it).atZone(zoneId).toLocalTime() }
                val averageSeconds = times.map { it.toSecondOfDay() }.average().toInt()
                predictedTime = LocalTime.ofSecondOfDay(averageSeconds.toLong())
            }

            // Analyze next day events
            val now = Instant.now().atZone(zoneId)
            val tomorrow = now.plusDays(1).toLocalDate()
            val events =
                calendarRepository.getEventsForMonth(
                    YearMonth.from(tomorrow),
                ).filter {
                    val eventDate = Instant.ofEpochMilli(it.startTime).atZone(zoneId).toLocalDate()
                    eventDate == tomorrow
                }.sortedBy { it.startTime }

            val firstEvent = events.firstOrNull() ?: return predictedTime

            // Check if we need to adjust for the first event
            val departureInfo = calculateDepartureTimeUseCase(firstEvent)
            val startTime = departureInfo?.departureTime ?: firstEvent.startTime
            val firstEventTime = Instant.ofEpochMilli(startTime).atZone(zoneId).toLocalTime()

            // Routine: Wake up 1 hour before first event/departure (arbitrary buffer)
            val suggestedWakeUp = firstEventTime.minusHours(1)

            return if (predictedTime == null || suggestedWakeUp.isBefore(predictedTime)) {
                suggestedWakeUp
            } else {
                predictedTime
            }
        }
    }
