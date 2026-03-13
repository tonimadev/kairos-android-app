package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.DirectionsRepository
import digital.tonima.core.repository.LocationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface CalculateDepartureTimeUseCase {
    suspend operator fun invoke(event: Event): DepartureInfo?
}

data class DepartureInfo(
    val departureTime: Long,
    val travelTimeMinutes: Int,
)

@BindType(installIn = BindType.Component.SINGLETON, to = CalculateDepartureTimeUseCase::class)
@Singleton
class CalculateDepartureTimeUseCaseImpl
    @Inject
    constructor(
        private val locationRepository: LocationRepository,
        private val directionsRepository: DirectionsRepository,
        private val appPreferencesRepository: AppPreferencesRepository,
    ) : CalculateDepartureTimeUseCase {
        override suspend fun invoke(event: Event): DepartureInfo? {
            val destination = event.location ?: return null

            // Check if feature is enabled
            if (!appPreferencesRepository.isLocationAlarmEnabled().first()) return null

            return locationRepository.getCurrentLocation()?.let { origin ->
                val mode = appPreferencesRepository.getPreferredTransportMode().first()

                directionsRepository.getTravelTimeSeconds(origin, destination, mode)?.let { travelTimeSeconds ->
                    // Buffer of 5 minutes (300 seconds)
                    val bufferSeconds = 300
                    val totalSecondsToSubtract = travelTimeSeconds + bufferSeconds

                    val departureTime = event.startTime - (totalSecondsToSubtract * 1000L)
                    val travelTimeMinutes = (travelTimeSeconds / 60)

                    DepartureInfo(departureTime, travelTimeMinutes)
                }
            }
        }
    }
