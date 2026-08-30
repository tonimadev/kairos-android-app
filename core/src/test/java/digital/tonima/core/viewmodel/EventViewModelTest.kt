package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import digital.tonima.core.usecases.AppPreferences
import digital.tonima.core.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.usecases.CancelEventAlarmUseCase
import digital.tonima.core.usecases.CheckPermissionsUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.usecases.GetCurrentLocationUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.usecases.GetMeetingTimeStatsUseCase
import digital.tonima.core.usecases.GetWeatherUseCase
import digital.tonima.core.usecases.LogEventUseCase
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.PermissionState
import digital.tonima.core.usecases.ScheduleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventVibrateUseCase
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
                mockCreateEventUseCase(any(), any(), any(), any(), any(), any(), any(), any())
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
                mockCreateEventUseCase(1, "New Event", "Desc", "Loc", 1000L, 2000L, false, any())
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
}
