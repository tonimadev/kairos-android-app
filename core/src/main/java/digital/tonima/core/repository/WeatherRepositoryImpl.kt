package digital.tonima.core.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.model.Weather
import digital.tonima.kairos.core.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@BindType(installIn = BindType.Component.SINGLETON, to = WeatherRepository::class)
@Singleton
class WeatherRepositoryImpl
    @Inject
    constructor() : WeatherRepository {
        private val json = Json { ignoreUnknownKeys = true }

        private val okHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    },
                )
                .build()

        private val retrofit =
            Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

        private val service = retrofit.create(OpenWeatherService::class.java)

        override suspend fun getWeather(
            city: String,
            isCelsius: Boolean,
        ): Weather? {
            if (BuildConfig.OPENWEATHER_API_KEY.isEmpty()) return null
            val units = if (isCelsius) "metric" else "imperial"
            return try {
                val response = service.getCurrentWeatherByCity(city, BuildConfig.OPENWEATHER_API_KEY, units)
                Weather(
                    temperature = response.main.temp,
                    description = response.weather.firstOrNull()?.description ?: "",
                    icon = response.weather.firstOrNull()?.icon ?: "",
                    city = response.name,
                )
            } catch (e: Exception) {
                null
            }
        }

        override suspend fun getWeather(
            lat: Double,
            lon: Double,
            isCelsius: Boolean,
        ): Weather? {
            if (BuildConfig.OPENWEATHER_API_KEY.isEmpty()) return null
            val units = if (isCelsius) "metric" else "imperial"
            return try {
                val response = service.getCurrentWeatherByCoords(lat, lon, BuildConfig.OPENWEATHER_API_KEY, units)
                Weather(
                    temperature = response.main.temp,
                    description = response.weather.firstOrNull()?.description ?: "",
                    icon = response.weather.firstOrNull()?.icon ?: "",
                    city = response.name,
                )
            } catch (e: Exception) {
                null
            }
        }
    }
