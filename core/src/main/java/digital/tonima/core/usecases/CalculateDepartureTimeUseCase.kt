package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.DirectionsRepository
import digital.tonima.core.repository.LocationRepository
import digital.tonima.core.repository.WeatherRepository
import digital.tonima.core.util.toOpenWeatherLang
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
        private val weatherRepository: WeatherRepository,
    ) : CalculateDepartureTimeUseCase {
        override suspend fun invoke(event: Event): DepartureInfo? {
            val destination = event.location ?: return null

            // Check if feature is enabled
            if (!appPreferencesRepository.isLocationAlarmEnabled().first()) return null

            return locationRepository.getCurrentLocation()?.let { origin ->
                val mode = appPreferencesRepository.getPreferredTransportMode().first()

                directionsRepository.getTravelTimeSeconds(origin, destination, mode)?.let { travelTimeSeconds ->
                    // Base buffer of 5 minutes (300 seconds)
                    var bufferSeconds = 300

                    // AI Integration: Weather check
                    try {
                        val coords = origin.split(",")
                        if (coords.size == 2) {
                            val lang = java.util.Locale.getDefault().toOpenWeatherLang()
                            val weather =
                                weatherRepository.getWeather(
                                    coords[0].toDouble(),
                                    coords[1].toDouble(),
                                    lang = lang,
                                )
                            // If it's raining or snowing, double the buffer
                            val conditionCode = weather?.conditionCode ?: 800
                            // 2xx: Thunderstorm, 3xx: Drizzle, 5xx: Rain, 6xx: Snow
                            if (conditionCode in 200..699) {
                                bufferSeconds += 600 // Add extra 10 minutes
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore weather errors, use base buffer
                    }

                    val totalSecondsToSubtract = travelTimeSeconds + bufferSeconds

                    val departureTime = event.startTime - (totalSecondsToSubtract * 1000L)
                    val travelTimeMinutes = (travelTimeSeconds / 60)

                    DepartureInfo(departureTime, travelTimeMinutes)
                }
            }
        }
    }
