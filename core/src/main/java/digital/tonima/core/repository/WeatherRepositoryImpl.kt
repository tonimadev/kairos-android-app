package digital.tonima.core.repository

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.model.Weather
import digital.tonima.kairos.core.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@BindType(installIn = BindType.Component.SINGLETON, to = WeatherRepository::class)
@Singleton
class WeatherRepositoryImpl
    @Inject
    constructor(
        private val service: OpenWeatherService,
    ) : WeatherRepository {
        override suspend fun getWeather(
            city: String,
            isCelsius: Boolean,
            lang: String,
        ): Weather? {
            if (BuildConfig.OPENWEATHER_API_KEY.isEmpty()) return null
            val units = if (isCelsius) "metric" else "imperial"
            return try {
                val response = service.getCurrentWeatherByCity(city, BuildConfig.OPENWEATHER_API_KEY, units, lang)
                Weather(
                    temperature = response.main.temp,
                    description = response.weather.firstOrNull()?.description ?: "",
                    icon = response.weather.firstOrNull()?.icon ?: "",
                    city = response.name,
                    conditionCode = response.weather.firstOrNull()?.id ?: 800,
                )
            } catch (e: Exception) {
                null
            }
        }

        override suspend fun getWeather(
            lat: Double,
            lon: Double,
            isCelsius: Boolean,
            lang: String,
        ): Weather? {
            if (BuildConfig.OPENWEATHER_API_KEY.isEmpty()) return null
            val units = if (isCelsius) "metric" else "imperial"
            return try {
                val response = service.getCurrentWeatherByCoords(lat, lon, BuildConfig.OPENWEATHER_API_KEY, units, lang)
                Weather(
                    temperature = response.main.temp,
                    description = response.weather.firstOrNull()?.description ?: "",
                    icon = response.weather.firstOrNull()?.icon ?: "",
                    city = response.name,
                    conditionCode = response.weather.firstOrNull()?.id ?: 800,
                )
            } catch (e: Exception) {
                null
            }
        }
    }
