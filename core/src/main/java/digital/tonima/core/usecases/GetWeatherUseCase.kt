package digital.tonima.core.usecases

import digital.tonima.core.repository.WeatherRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetWeatherUseCase
    @Inject
    constructor(
        private val weatherRepository: WeatherRepository,
    ) {
        suspend operator fun invoke(
            lat: Double,
            lon: Double,
            isCelsius: Boolean,
            lang: String,
        ) = weatherRepository.getWeather(lat, lon, isCelsius, lang)
    }
