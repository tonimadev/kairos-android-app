package digital.tonima.core.repository

import digital.tonima.core.model.OpenWeatherResponse
import digital.tonima.core.model.Weather
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherRepository {
    suspend fun getWeather(city: String): Weather?

    suspend fun getWeather(
        lat: Double,
        lon: Double,
    ): Weather?
}

interface OpenWeatherService {
    @GET("weather")
    suspend fun getCurrentWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pt_br",
    ): OpenWeatherResponse

    @GET("weather")
    suspend fun getCurrentWeatherByCoords(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pt_br",
    ): OpenWeatherResponse
}
