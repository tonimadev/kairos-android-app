package digital.tonima.core.usecases

import digital.tonima.core.analytics.Analytics
import digital.tonima.core.analytics.EventAnalytics
import digital.tonima.core.data.repository.LocationRepository
import digital.tonima.core.data.repository.WeatherRepository
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.core.viewmodel.EventIntent
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.core.model.Event
import digital.tonima.kairos.core.model.Weather
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarDelegatingUseCasesTest {
    private val scheduler: EventAlarmScheduler = mockk(relaxed = true)
    private val analytics: Analytics = mockk(relaxed = true)
    private val event = Event(id = 1L, title = "Reunião", startTime = 1_000L)

    @Test
    fun `alarm use cases delegate to the scheduler`() {
        ScheduleEventAlarmUseCase(scheduler)(event, 500L)
        CancelEventAlarmUseCase(scheduler)(event)

        verify { scheduler.schedule(event, 500L) }
        verify { scheduler.cancel(event) }
    }

    @Test
    fun `location and weather come from their repositories`() =
        runTest {
            val weather = Weather(20.0, "limpo", "01d", "Natal", 800)
            val location = mockk<LocationRepository> { coEvery { getCurrentLocation() } returns "1.0,2.0" }
            val weatherRepository =
                mockk<WeatherRepository> { coEvery { getWeather(1.0, 2.0, false, "pt") } returns weather }

            assertEquals("1.0,2.0", GetCurrentLocationUseCase(location)())
            assertEquals(weather, GetWeatherUseCase(weatherRepository)(1.0, 2.0, false, "pt"))
        }

    @Test
    fun `user actions are logged with their parameters`() {
        val log = LogEventUseCase(EventAnalytics(analytics))
        val uiEvent = EventUiModel(id = 1L, title = "Reunião", startTime = 1_000L)

        log(EventIntent.JoinMeeting("https://meet.google.com/x"))
        log(EventIntent.ToggleEventAlarm(uiEvent, enabled = false, allOccurrences = true))
        log(EventIntent.ToggleCalendarFilter(7L, enabled = true))
        log(EventIntent.UpgradeToProRequest)
        log(EventIntent.UpgradeToProIARequest)
        log(EventIntent.RateNow)
        log(EventIntent.RateLater)
        log(EventIntent.RateNever)
        log.logEventCreated()

        verify { analytics.logEvent(Analytics.EVENT_JOIN_MEETING) }
        verify {
            analytics.logEvent(
                Analytics.EVENT_ALARM_TOGGLE,
                mapOf(Analytics.PARAM_ENABLED to false, Analytics.PARAM_ALL_OCCURRENCES to true),
            )
        }
        verify {
            analytics.logEvent(
                Analytics.EVENT_CALENDAR_FILTER_TOGGLE,
                mapOf(Analytics.PARAM_CALENDAR_ID to 7L, Analytics.PARAM_ENABLED to true),
            )
        }
        verify { analytics.logEvent(Analytics.EVENT_UPGRADE_REQUEST) }
        verify { analytics.logEvent(Analytics.EVENT_UPGRADE_IA_REQUEST) }
        verify { analytics.logEvent(Analytics.EVENT_RATE_NOW) }
        verify { analytics.logEvent(Analytics.EVENT_RATE_LATER) }
        verify { analytics.logEvent(Analytics.EVENT_RATE_NEVER) }
        verify { analytics.logEvent(Analytics.EVENT_EVENT_CREATED) }
    }

    @Test
    fun `navigation intents are not logged`() {
        LogEventUseCase(EventAnalytics(analytics))(EventIntent.RefreshEvents)

        verify(exactly = 0) { analytics.logEvent(any(), any()) }
    }
}
