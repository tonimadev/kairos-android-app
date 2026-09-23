package digital.tonima.core.data.repository

import digital.tonima.core.di.NetworkModule
import digital.tonima.kairos.core.BuildConfig
import digital.tonima.kairos.core.model.Weather
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Runs the real Retrofit services from [NetworkModule] against canned HTTP responses, so the
 * request parameters, JSON models and repository mapping are exercised together without network.
 */
class NetworkRepositoriesTest {
    private val requests = mutableListOf<okhttp3.HttpUrl>()
    private var responseCode = 200
    private var responseBody = ""

    private val client: OkHttpClient =
        NetworkModule.provideOkHttpClient()
            .newBuilder()
            .addInterceptor(
                Interceptor { chain ->
                    requests += chain.request().url
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(responseCode)
                        .message("canned")
                        .body(responseBody.toResponseBody("application/json".toMediaType()))
                        .build()
                },
            )
            .build()

    private val weatherRepository =
        WeatherRepositoryImpl(NetworkModule.provideOpenWeatherService(client, NetworkModule.provideJson()))
    private val directionsRepository =
        DirectionsRepositoryImpl(NetworkModule.provideDistanceMatrixService(client, NetworkModule.provideJson()))

    @Test
    fun `weather by city is mapped from the OpenWeather answer`() =
        runTest {
            assumeTrue(BuildConfig.OPENWEATHER_API_KEY.isNotEmpty())
            responseBody =
                """{"main":{"temp":27.4,"humidity":80},"name":"Recife",""" +
                """"weather":[{"id":500,"description":"chuva fraca","icon":"10d"}]}"""

            val weather = weatherRepository.getWeather("Recife", isCelsius = true, lang = "pt_br")

            assertEquals(Weather(27.4, "chuva fraca", "10d", "Recife", 500), weather)
            val url = requests.single()
            assertEquals("Recife", url.queryParameter("q"))
            assertEquals("metric", url.queryParameter("units"))
            assertEquals("pt_br", url.queryParameter("lang"))
        }

    @Test
    fun `weather by coordinates uses imperial units for Fahrenheit`() =
        runTest {
            assumeTrue(BuildConfig.OPENWEATHER_API_KEY.isNotEmpty())
            responseBody = """{"main":{"temp":80.1},"weather":[],"name":"Miami"}"""

            val weather = weatherRepository.getWeather(25.7, -80.2, isCelsius = false)

            assertEquals(Weather(80.1, "", "", "Miami", 800), weather)
            val url = requests.single()
            assertEquals("25.7", url.queryParameter("lat"))
            assertEquals("imperial", url.queryParameter("units"))
            assertEquals("en", url.queryParameter("lang"))
        }

    @Test
    fun `weather failures are swallowed`() =
        runTest {
            assumeTrue(BuildConfig.OPENWEATHER_API_KEY.isNotEmpty())
            responseCode = 500

            assertNull(weatherRepository.getWeather("Recife"))
            assertNull(weatherRepository.getWeather(1.0, 2.0))
        }

    @Test
    fun `without an OpenWeather key nothing is requested`() =
        runTest {
            assumeTrue(BuildConfig.OPENWEATHER_API_KEY.isEmpty())

            assertNull(weatherRepository.getWeather("Recife"))
            assertNull(weatherRepository.getWeather(1.0, 2.0))
            assertEquals(emptyList<okhttp3.HttpUrl>(), requests)
        }

    @Test
    fun `travel time comes from the first route element`() =
        runTest {
            assumeTrue(BuildConfig.GOOGLE_MAPS_API_KEY.isNotEmpty())
            responseBody =
                """{"status":"OK","rows":[{"elements":[{"status":"OK","duration":{"text":"25 min","value":1500}}]}]}"""

            assertEquals(1500, directionsRepository.getTravelTimeSeconds("A", "B", "driving"))
            assertEquals("now", requests.single().queryParameter("departure_time"))
        }

    @Test
    fun `travel time is unknown when Google cannot route`() =
        runTest {
            assumeTrue(BuildConfig.GOOGLE_MAPS_API_KEY.isNotEmpty())

            responseBody = """{"status":"OK","rows":[{"elements":[{"status":"ZERO_RESULTS"}]}]}"""
            assertNull(directionsRepository.getTravelTimeSeconds("A", "B", "walking"))

            responseBody = """{"status":"REQUEST_DENIED","rows":[]}"""
            assertNull(directionsRepository.getTravelTimeSeconds("A", "B", "walking"))

            responseCode = 500
            assertNull(directionsRepository.getTravelTimeSeconds("A", "B", "walking"))
        }

    @Test
    fun `without a Maps key nothing is requested`() =
        runTest {
            assumeTrue(BuildConfig.GOOGLE_MAPS_API_KEY.isEmpty())

            assertNull(directionsRepository.getTravelTimeSeconds("A", "B", "driving"))
            assertEquals(emptyList<okhttp3.HttpUrl>(), requests)
        }
}
