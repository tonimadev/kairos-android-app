package digital.tonima.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Weather(
    val temperature: Double,
    val description: String,
    val icon: String,
    val city: String,
    val conditionCode: Int,
)

@Serializable
data class OpenWeatherResponse(
    val main: Main,
    val weather: List<WeatherDescription>,
    val name: String,
)

@Serializable
data class Main(
    val temp: Double,
)

@Serializable
data class WeatherDescription(
    val id: Int,
    val description: String,
    val icon: String,
)
