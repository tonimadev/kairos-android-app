package digital.tonima.core.data.usecases

import digital.tonima.core.data.repository.DirectionsRepository
import digital.tonima.core.data.repository.LocationRepository
import digital.tonima.core.data.repository.WeatherRepository
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.kairos.core.model.Event
import digital.tonima.kairos.core.model.Weather
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            coEvery { mockWeatherRepository.getWeather(-23.5505, -46.6333, any(), any()) } returns
                Weather(
                    temperature = 25.0,
                    description = "Clear",
                    icon = "01d",
                    city = "SP",
                    conditionCode = 800,
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
                    any(),
                    any(),
                )
            } returns
                Weather(
                    temperature = 20.0,
                    description = "chuva leve",
                    icon = "10d",
                    city = "SP",
                    conditionCode = 500,
                )

            val result = useCase(event)

            assertNotNull(result)
            // 30 min travel + 5 min base buffer + 10 min weather buffer = 45 min = 2700 sec
            val expectedDeparture = eventTime - (2700 * 1000L)
            assertEquals(expectedDeparture, result?.departureTime)
        }

    @Test
    fun `when event has no location then returns null without checking other repositories`() =
        runTest {
            val event = Event(id = 1, title = "Meeting", startTime = 1715558400000L, location = null)

            val result = useCase(event)

            assertNull(result)
            coVerify(exactly = 0) { mockLocationRepository.getCurrentLocation() }
        }

    @Test
    fun `when location alarm feature is disabled then returns null`() =
        runTest {
            coEvery { mockAppPreferencesRepository.isLocationAlarmEnabled() } returns flowOf(false)
            val event = Event(id = 1, title = "Meeting", startTime = 1715558400000L, location = "Office")

            val result = useCase(event)

            assertNull(result)
            coVerify(exactly = 0) { mockLocationRepository.getCurrentLocation() }
        }

    @Test
    fun `when current location is unavailable then returns null`() =
        runTest {
            val event = Event(id = 1, title = "Meeting", startTime = 1715558400000L, location = "Office")
            coEvery { mockLocationRepository.getCurrentLocation() } returns null

            val result = useCase(event)

            assertNull(result)
            coVerify(exactly = 0) { mockDirectionsRepository.getTravelTimeSeconds(any(), any(), any()) }
        }

    @Test
    fun `when travel time is unavailable then returns null`() =
        runTest {
            val event = Event(id = 1, title = "Meeting", startTime = 1715558400000L, location = "Office")
            val origin = "-23.5505,-46.6333"
            coEvery { mockLocationRepository.getCurrentLocation() } returns origin
            coEvery {
                mockDirectionsRepository.getTravelTimeSeconds(origin, "Office", "driving")
            } returns null

            val result = useCase(event)

            assertNull(result)
        }

    @Test
    fun `when weather lookup throws then falls back to base buffer only`() =
        runTest {
            val eventTime = 1715558400000L
            val event = Event(id = 1, title = "Meeting", startTime = eventTime, location = "Office")
            val origin = "-23.5505,-46.6333"

            coEvery { mockLocationRepository.getCurrentLocation() } returns origin
            coEvery {
                mockDirectionsRepository.getTravelTimeSeconds(origin, "Office", "driving")
            } returns 1800
            coEvery {
                mockWeatherRepository.getWeather(-23.5505, -46.6333, any(), any())
            } throws RuntimeException("network error")

            val result = useCase(event)

            assertNotNull(result)
            // Weather lookup failed, so only the 5 min base buffer applies.
            val expectedDeparture = eventTime - (2100 * 1000L)
            assertEquals(expectedDeparture, result?.departureTime)
        }

    @Test
    fun `when origin has no comma separated coordinates then weather check is skipped`() =
        runTest {
            val eventTime = 1715558400000L
            val event = Event(id = 1, title = "Meeting", startTime = eventTime, location = "Office")
            val origin = "unknown-origin-format"

            coEvery { mockLocationRepository.getCurrentLocation() } returns origin
            coEvery {
                mockDirectionsRepository.getTravelTimeSeconds(origin, "Office", "driving")
            } returns 1800

            val result = useCase(event)

            assertNotNull(result)
            val expectedDeparture = eventTime - (2100 * 1000L)
            assertEquals(expectedDeparture, result?.departureTime)
            coVerify(exactly = 0) { mockWeatherRepository.getWeather(any(), any(), any(), any()) }
        }
}
