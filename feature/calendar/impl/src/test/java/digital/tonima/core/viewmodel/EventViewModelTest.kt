package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import digital.tonima.core.data.usecases.AppPreferences
import digital.tonima.core.data.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.data.usecases.CheckPermissionsUseCase
import digital.tonima.core.data.usecases.CreateEventUseCase
import digital.tonima.core.data.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.data.usecases.GetEventsForMonthUseCase
import digital.tonima.core.data.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.data.usecases.PermissionState
import digital.tonima.core.data.usecases.UpdateAppPreferenceUseCase
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.usecases.CancelEventAlarmUseCase
import digital.tonima.core.usecases.GetCurrentLocationUseCase
import digital.tonima.core.usecases.GetMeetingTimeStatsUseCase
import digital.tonima.core.usecases.GetWeatherUseCase
import digital.tonima.core.usecases.LogEventUseCase
import digital.tonima.core.usecases.ScheduleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventVibrateUseCase
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.core.model.DeviceCalendar
import digital.tonima.kairos.core.model.Event
import digital.tonima.kairos.core.model.Weather
import digital.tonima.kairos.core.navigation.AppNavigator
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.YearMonth

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class EventViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val mockProUserProvider: ProUserProvider = mockk(relaxed = true)
    private val mockGetEventsForMonthUseCase: GetEventsForMonthUseCase = mockk(relaxed = true)
    private val mockGetAvailableCalendarsUseCase: GetAvailableCalendarsUseCase = mockk(relaxed = true)
    private val mockCreateEventUseCase: CreateEventUseCase = mockk(relaxed = true)
    private val mockObserveAppPreferencesUseCase: ObserveAppPreferencesUseCase = mockk(relaxed = true)
    private val mockUpdateAppPreferenceUseCase: UpdateAppPreferenceUseCase = mockk(relaxed = true)
    private val mockToggleEventAlarmUseCase: ToggleEventAlarmUseCase = mockk(relaxed = true)
    private val mockToggleEventVibrateUseCase: ToggleEventVibrateUseCase = mockk(relaxed = true)
    private val mockScheduleEventAlarmUseCase: ScheduleEventAlarmUseCase = mockk(relaxed = true)
    private val mockCancelEventAlarmUseCase: CancelEventAlarmUseCase = mockk(relaxed = true)
    private val mockCalculateDepartureTimeUseCase: CalculateDepartureTimeUseCase = mockk(relaxed = true)
    private val mockLogEventUseCase: LogEventUseCase = mockk(relaxed = true)
    private val mockCheckPermissionsUseCase: CheckPermissionsUseCase = mockk(relaxed = true)
    private val mockGetCurrentLocationUseCase: GetCurrentLocationUseCase = mockk(relaxed = true)
    private val mockGetWeatherUseCase: GetWeatherUseCase = mockk(relaxed = true)
    private val mockGetMeetingTimeStatsUseCase: GetMeetingTimeStatsUseCase = mockk(relaxed = true)
    private val mockAppNavigator: AppNavigator = mockk(relaxed = true)

    private val appPreferencesFlow = MutableStateFlow(defaultAppPreferences())
    private val isProUserFlow = MutableStateFlow(false)
    private val isAiUserFlow = MutableStateFlow(false)

    private lateinit var viewModel: EventViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { mockProUserProvider.isProUser } returns isProUserFlow
        every { mockProUserProvider.isAiUser } returns isAiUserFlow
        every { mockObserveAppPreferencesUseCase() } returns appPreferencesFlow
        every { mockCheckPermissionsUseCase() } returns
            PermissionState(
                hasCalendarPermission = true,
                hasPostNotificationsPermission = true,
                hasExactAlarmPermission = true,
                hasFullScreenIntentPermission = true,
                hasLocationPermission = false,
                hasBackgroundLocationPermission = false,
            )
        coEvery { mockGetEventsForMonthUseCase(any()) } returns emptyList()
        coEvery { mockGetAvailableCalendarsUseCase() } returns emptyList()

        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun defaultAppPreferences() =
        AppPreferences(
            isGlobalAlarmEnabled = true,
            vibrateOnly = false,
            allDayAlarmsEnabled = true,
            allDayAlarmHour = 9,
            alarmOffsetMinutes = 0L,
            isLocationAlarmEnabled = false,
            preferredTransportMode = "driving",
            enabledCalendarIds = emptySet(),
            snoozeTimeMinutes = 10,
            autostartSuggestionDismissed = false,
            disabledEventIds = emptySet(),
            disabledSeriesIds = emptySet(),
            vibrateOnlyEventIds = emptySet(),
            exactAlarmPermissionSkipped = false,
            fullScreenIntentPermissionSkipped = false,
            skipWeekendsEnabled = false,
            autoDismissMinutes = 5,
            isTemperatureInCelsius = true,
            isAutoJoinEnabled = false,
            isAutoFocusModeEnabled = false,
        )

    private fun createViewModel() =
        EventViewModel(
            proUserProvider = mockProUserProvider,
            getEventsForMonthUseCase = mockGetEventsForMonthUseCase,
            getAvailableCalendarsUseCase = mockGetAvailableCalendarsUseCase,
            createEventUseCase = mockCreateEventUseCase,
            observeAppPreferencesUseCase = mockObserveAppPreferencesUseCase,
            updateAppPreferenceUseCase = mockUpdateAppPreferenceUseCase,
            toggleEventAlarmUseCase = mockToggleEventAlarmUseCase,
            toggleEventVibrateUseCase = mockToggleEventVibrateUseCase,
            scheduleEventAlarmUseCase = mockScheduleEventAlarmUseCase,
            cancelEventAlarmUseCase = mockCancelEventAlarmUseCase,
            calculateDepartureTimeUseCase = mockCalculateDepartureTimeUseCase,
            logEventUseCase = mockLogEventUseCase,
            checkPermissionsUseCase = mockCheckPermissionsUseCase,
            getCurrentLocationUseCase = mockGetCurrentLocationUseCase,
            getWeatherUseCase = mockGetWeatherUseCase,
            getMeetingTimeStatsUseCase = mockGetMeetingTimeStatsUseCase,
            appNavigator = mockAppNavigator,
        )

    @Test
    fun `onDateSelected updates selectedDate in UI state`() =
        runTest {
            val newDate = LocalDate.of(2023, 10, 26)
            val newDateEpoch = newDate.toEpochDay()
            viewModel.handleIntent(EventIntent.SelectDate(newDateEpoch))
            advanceUntilIdle()

            assertEquals(newDateEpoch, viewModel.uiState.value.selectedDate)
        }

    @Test
    fun `returnToToday updates selectedDate and currentMonth`() =
        runTest {
            val oldDate = LocalDate.of(2000, 1, 1).toEpochDay()
            viewModel.handleIntent(EventIntent.SelectDate(oldDate))
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.ReturnToToday)
            advanceUntilIdle()

            val today = LocalDate.now().toEpochDay()
            val currentMonth = YearMonth.now().atDay(1).toEpochDay()
            assertEquals(today, viewModel.uiState.value.selectedDate)
            assertEquals(currentMonth, viewModel.uiState.value.currentMonth)
        }

    @Test
    fun `onMonthChanged without calendar permission clears events`() =
        runTest {
            runCurrent()

            every { mockCheckPermissionsUseCase() } returns
                PermissionState(
                    hasCalendarPermission = false,
                    hasPostNotificationsPermission = true,
                    hasExactAlarmPermission = true,
                    hasFullScreenIntentPermission = true,
                    hasLocationPermission = false,
                    hasBackgroundLocationPermission = false,
                )

            clearMocks(mockGetEventsForMonthUseCase, answers = false)

            viewModel = createViewModel()
            runCurrent()

            val targetMonth = YearMonth.of(2024, 10).atDay(1).toEpochDay()
            viewModel.handleIntent(EventIntent.ChangeMonth(targetMonth))
            runCurrent()

            assertTrue(viewModel.uiState.value.events.isEmpty())
            coVerify(exactly = 0) { mockGetEventsForMonthUseCase(any()) }
        }

    @Test
    fun `loadAvailableCalendars updates UI state`() =
        runTest {
            val mockCalendars = listOf(DeviceCalendar(1, "Calendar 1", "Account 1"))
            coEvery { mockGetAvailableCalendarsUseCase() } returns mockCalendars

            viewModel.handleIntent(EventIntent.LoadCalendars)
            advanceUntilIdle()

            assertEquals(mockCalendars, viewModel.uiState.value.availableCalendars)
        }

    @Test
    fun `CreateEvent intent calls use case and shows success snackbar`() =
        runTest {
            coEvery {
                mockCreateEventUseCase(any(), any(), any(), any(), any(), any(), any())
            } returns 123L

            viewModel.handleIntent(
                EventIntent.CreateEvent(
                    calendarId = 1,
                    title = "New Event",
                    description = "Desc",
                    location = "Loc",
                    startTime = 1000L,
                    endTime = 2000L,
                    isAllDay = false,
                ),
            )
            advanceUntilIdle()

            val effect = viewModel.uiState.value.effect
            assertTrue(effect is EventSideEffect.ShowSnackbar)

            coVerify {
                mockCreateEventUseCase(1, "New Event", "Desc", "Loc", 1000L, 2000L, false)
            }
        }

    @Test
    fun `global alarm disabled after events loaded cancels all loaded alarms`() =
        runTest {
            val now = System.currentTimeMillis()
            val e1 = Event(id = 301, title = "A", startTime = now + 5 * 60 * 1000L)
            val e2 = Event(id = 302, title = "B", startTime = now + 6 * 60 * 1000L)
            coEvery { mockGetEventsForMonthUseCase(any()) } returns listOf(e1, e2)

            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.now().atDay(1).toEpochDay()))
            advanceUntilIdle()

            appPreferencesFlow.value = defaultAppPreferences().copy(isGlobalAlarmEnabled = false)
            advanceUntilIdle()

            verify { mockCancelEventAlarmUseCase(match { it.id == 301L }) }
            verify { mockCancelEventAlarmUseCase(match { it.id == 302L }) }
        }

    @Test
    fun `overlapping timed events are flagged as conflicts`() =
        runTest {
            val base = System.currentTimeMillis() + 3_600_000L
            val a = Event(id = 1, title = "A", startTime = base, endTime = base + 3_600_000L)
            val b = Event(id = 2, title = "B", startTime = base + 1_800_000L, endTime = base + 5_400_000L)
            val c = Event(id = 3, title = "C", startTime = base + 10_800_000L, endTime = base + 14_400_000L)

            val events = loadEvents(a, b, c)

            assertTrue(events.getValue(1).hasConflict)
            assertTrue(events.getValue(2).hasConflict)
            assertFalse(events.getValue(3).hasConflict)
        }

    @Test
    fun `events less than five minutes apart are flagged as back to back`() =
        runTest {
            val base = System.currentTimeMillis() + 3_600_000L
            val first = Event(id = 1, title = "First", startTime = base, endTime = base + 3_600_000L)
            val next =
                Event(id = 2, title = "Next", startTime = base + 3_600_000L + 240_000L, endTime = base + 7_200_000L)
            val later = Event(id = 3, title = "Later", startTime = base + 10_800_000L, endTime = base + 14_400_000L)

            val events = loadEvents(first, next, later)

            assertTrue(events.getValue(1).isBackToBack)
            assertTrue(events.getValue(2).isBackToBack)
            assertFalse(events.getValue(3).isBackToBack)
            assertFalse(events.getValue(1).hasConflict)
        }

    @Test
    fun `all-day events never conflict with timed events`() =
        runTest {
            val base = System.currentTimeMillis() + 3_600_000L
            val allDay =
                Event(id = 1, title = "Holiday", startTime = base, endTime = base + 86_400_000L, isAllDay = true)
            val meeting = Event(id = 2, title = "Meeting", startTime = base + 3_600_000L, endTime = base + 7_200_000L)

            val events = loadEvents(allDay, meeting)

            assertFalse(events.getValue(1).hasConflict)
            assertFalse(events.getValue(2).hasConflict)
        }

    @Test
    fun `weather is fetched for the current location in the chosen unit`() =
        runTest {
            val weather =
                Weather(temperature = 25.0, description = "clear", icon = "01d", city = "SP", conditionCode = 800)
            coEvery { mockGetCurrentLocationUseCase() } returns "-23.55,-46.63"
            coEvery { mockGetWeatherUseCase(-23.55, -46.63, true, any()) } returns weather

            viewModel.handleIntent(EventIntent.FetchWeather)
            advanceUntilIdle()

            assertEquals(weather, viewModel.uiState.value.weather)
            assertFalse(viewModel.uiState.value.isWeatherLoading)
            assertNull(viewModel.uiState.value.weatherError)
        }

    @Test
    fun `weather reports why it could not be loaded`() =
        runTest {
            listOf(null, "not-a-location", "abc,def", "-23.55,-46.63").forEach { location ->
                coEvery { mockGetCurrentLocationUseCase() } returns location
                coEvery { mockGetWeatherUseCase(any(), any(), any(), any()) } returns null

                viewModel.handleIntent(EventIntent.FetchWeather)
                advanceUntilIdle()

                assertNotNull("location=$location", viewModel.uiState.value.weatherError)
                assertFalse(viewModel.uiState.value.isWeatherLoading)
                assertNull(viewModel.uiState.value.weather)
            }
        }

    @Test
    fun `rating now remembers it and asks the store for a review`() =
        runTest {
            viewModel.handleIntent(EventIntent.RateNow)
            advanceUntilIdle()

            coVerify { mockUpdateAppPreferenceUseCase.setRatingCompleted(true) }
            assertFalse(viewModel.uiState.value.showRatingBottomSheet)
            assertEquals(EventSideEffect.RequestAppReview, viewModel.uiState.value.effect)
        }

    private fun TestScope.loadEvents(vararg events: Event): Map<Long, EventUiModel> {
        coEvery { mockGetEventsForMonthUseCase(any()) } returns events.toList()
        viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.now().atDay(1).toEpochDay()))
        advanceUntilIdle()
        return viewModel.uiState.value.events.associateBy { it.id }
    }
}
