package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import digital.tonima.core.model.Weather
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.DirectionsRepository
import digital.tonima.core.repository.LocationRepository
import digital.tonima.core.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class CalculateDepartureTimeUseCaseTest {
    private val mockLocationRepository = mockk<LocationRepository>()
    private val mockDirectionsRepository = mockk<DirectionsRepository>()
    private val mockAppPreferencesRepository = mockk<AppPreferencesRepository>()
    private val mockWeatherRepository = mockk<WeatherRepository>()

    private lateinit var useCase: CalculateDepartureTimeUseCaseImpl

    @Before
    fun setup() {
        useCase =
            CalculateDepartureTimeUseCaseImpl(
                mockLocationRepository,
                mockDirectionsRepository,
                mockAppPreferencesRepository,
                mockWeatherRepository,
            )
        coEvery { mockAppPreferencesRepository.isLocationAlarmEnabled() } returns flowOf(true)
        coEvery { mockAppPreferencesRepository.getPreferredTransportMode() } returns flowOf("driving")
    }

    @Test
    fun `when weather is clear then use base buffer 5min`() =
        runTest {
            val eventTime = 1715558400000L // arbitrary
            val event = Event(id = 1, title = "Meeting", startTime = eventTime, location = "Office")
            val origin = "-23.5505,-46.6333" // SP

            coEvery { mockLocationRepository.getCurrentLocation() } returns origin
            coEvery {
                mockDirectionsRepository.getTravelTimeSeconds(
                    origin,
                    "Office",
                    "driving",
                )
            } returns 1800 // 30 min
            coEvery { mockWeatherRepository.getWeather(-23.5505, -46.6333) } returns
                Weather(
                    25.0,
                    "Clear",
                    "01d",
                    "SP",
                )

            val result = useCase(event)

            assertNotNull(result)
            // 30 min travel + 5 min buffer = 35 min = 2100 sec
            val expectedDeparture = eventTime - (2100 * 1000L)
            assertEquals(expectedDeparture, result?.departureTime)
            assertEquals(30, result?.travelTimeMinutes)
        }

    @Test
    fun `when it is raining then add 10min extra buffer`() =
        runTest {
            val eventTime = 1715558400000L
            val event = Event(id = 1, title = "Meeting", startTime = eventTime, location = "Office")
            val origin = "-23.5505,-46.6333"

            coEvery { mockLocationRepository.getCurrentLocation() } returns origin
            coEvery {
                mockDirectionsRepository.getTravelTimeSeconds(
                    origin,
                    "Office",
                    "driving",
                )
            } returns 1800 // 30 min
            coEvery {
                mockWeatherRepository.getWeather(
                    -23.5505,
                    -46.6333,
                )
            } returns Weather(20.0, "chuva leve", "10d", "SP")

            val result = useCase(event)

            assertNotNull(result)
            // 30 min travel + 5 min base buffer + 10 min weather buffer = 45 min = 2700 sec
            val expectedDeparture = eventTime - (2700 * 1000L)
            assertEquals(expectedDeparture, result?.departureTime)
        }
}
