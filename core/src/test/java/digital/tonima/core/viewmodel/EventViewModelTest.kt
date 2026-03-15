package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.review.ReviewManager
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.core.usecases.AppPreferences
import digital.tonima.core.usecases.AskAiAboutScheduleUseCase
import digital.tonima.core.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.usecases.CheckPermissionsUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.ObserveDailyBriefingUseCase
import digital.tonima.core.usecases.ObserveRingerModeUseCase
import digital.tonima.core.usecases.PermissionState
import digital.tonima.core.usecases.ToggleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventVibrateUseCase
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import digital.tonima.core.utils.TextToSpeechHelper
import digital.tonima.core.utils.WidgetUpdater
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
@Suppress("LargeClass")
class EventViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val mockProUserProvider: ProUserProvider = mockk(relaxed = true)
    private val mockGetEventsForMonthUseCase: GetEventsForMonthUseCase = mockk(relaxed = true)
    private val mockObserveAppPreferencesUseCase: ObserveAppPreferencesUseCase = mockk(relaxed = true)
    private val mockUpdateAppPreferenceUseCase: UpdateAppPreferenceUseCase = mockk(relaxed = true)
    private val mockToggleEventAlarmUseCase: ToggleEventAlarmUseCase = mockk(relaxed = true)
    private val mockToggleEventVibrateUseCase: ToggleEventVibrateUseCase = mockk(relaxed = true)
    private val mockObserveRingerModeUseCase: ObserveRingerModeUseCase = mockk(relaxed = true)
    private val mockGetAvailableCalendarsUseCase: GetAvailableCalendarsUseCase = mockk(relaxed = true)
    private val mockObserveDailyBriefingUseCase: ObserveDailyBriefingUseCase = mockk(relaxed = true)
    private val mockGenerateDailyBriefingUseCase: GenerateDailyBriefingUseCase = mockk(relaxed = true)
    private val mockAskAiAboutScheduleUseCase: AskAiAboutScheduleUseCase = mockk(relaxed = true)
    private val mockCreateEventUseCase: CreateEventUseCase = mockk(relaxed = true)
    private val mockCalculateDepartureTimeUseCase: CalculateDepartureTimeUseCase = mockk(relaxed = true)
    private val mockCheckPermissionsUseCase: CheckPermissionsUseCase = mockk(relaxed = true)
    private val mockTtsHelper: TextToSpeechHelper = mockk(relaxed = true)
    private val mockWidgetUpdater: WidgetUpdater = mockk(relaxed = true)
    private val mockScheduler: EventAlarmScheduler = mockk(relaxed = true)
    private val mockReviewManager: ReviewManager = mockk(relaxed = true)

    private val appPreferencesFlow = MutableStateFlow(defaultAppPreferences())
    private val ringerModeFlow = MutableStateFlow(AudioWarningState.NORMAL)
    private val dailyBriefingFlow = MutableStateFlow<String?>(null)
    private val isProUserFlow = MutableStateFlow(false)
    private val isAiUserFlow = MutableStateFlow(false)

    private lateinit var viewModel: EventViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { mockProUserProvider.isProUser } returns isProUserFlow
        every { mockProUserProvider.isAiUser } returns isAiUserFlow
        every { mockObserveAppPreferencesUseCase() } returns appPreferencesFlow
        every { mockObserveRingerModeUseCase() } returns ringerModeFlow
        every { mockObserveDailyBriefingUseCase() } returns dailyBriefingFlow
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
        )

    private fun createViewModel() =
        EventViewModel(
            proUserProvider = mockProUserProvider,
            calendar =
                EventViewModel.CalendarDeps(
                    getEventsForMonth = mockGetEventsForMonthUseCase,
                    getAvailableCalendars = mockGetAvailableCalendarsUseCase,
                    createEvent = mockCreateEventUseCase,
                ),
            prefs =
                EventViewModel.PreferencesDeps(
                    observe = mockObserveAppPreferencesUseCase,
                    update = mockUpdateAppPreferenceUseCase,
                ),
            alarm =
                EventViewModel.AlarmDeps(
                    toggleEventAlarm = mockToggleEventAlarmUseCase,
                    toggleEventVibrate = mockToggleEventVibrateUseCase,
                    scheduler = mockScheduler,
                ),
            ai =
                EventViewModel.AiDeps(
                    generateDailyBriefing = mockGenerateDailyBriefingUseCase,
                    askAiAboutSchedule = mockAskAiAboutScheduleUseCase,
                    calculateDepartureTime = mockCalculateDepartureTimeUseCase,
                    observeDailyBriefing = mockObserveDailyBriefingUseCase,
                    tts = mockTtsHelper,
                    widgetUpdater = mockWidgetUpdater,
                ),
            observeRingerModeUseCase = mockObserveRingerModeUseCase,
            checkPermissionsUseCase = mockCheckPermissionsUseCase,
            reviewManager = mockReviewManager,
        )

    @Test
    fun `checkAllPermissions updates all permission flags in UI state`() =
        runTest {
            advanceUntilIdle()

            every { mockCheckPermissionsUseCase() } returns
                PermissionState(
                    hasCalendarPermission = false,
                    hasPostNotificationsPermission = false,
                    hasExactAlarmPermission = true,
                    hasFullScreenIntentPermission = true,
                    hasLocationPermission = false,
                    hasBackgroundLocationPermission = false,
                )

            viewModel.uiState.test {
                skipItems(1)

                viewModel.handleIntent(EventIntent.CheckPermissions)
                advanceUntilIdle()

                val updatedState = awaitItem()
                assertFalse(updatedState.hasCalendarPermission)
                assertFalse(updatedState.hasPostNotificationsPermission)
                assertTrue(updatedState.hasExactAlarmPermission)
                assertTrue(updatedState.hasFullScreenIntentPermission)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `dismissAutostartSuggestion sets dismissed flag in preferences`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setAutostartSuggestionDismissed(true) } just Runs

            viewModel.handleIntent(EventIntent.DismissAutostartSuggestion)
            advanceUntilIdle()

            coVerify(exactly = 1) { mockUpdateAppPreferenceUseCase.setAutostartSuggestionDismissed(true) }
        }

    @Test
    fun `onDateSelected updates selectedDate in UI state`() =
        runTest {
            advanceUntilIdle()

            val newDate = LocalDate.of(2023, 10, 26)

            viewModel.uiState.test {
                skipItems(1)

                viewModel.handleIntent(EventIntent.SelectDate(newDate))
                advanceUntilIdle()

                val updatedState = awaitItem()
                assertEquals(newDate, updatedState.selectedDate)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onAlarmsToggle sets global alarm enabled status in preferences`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setGlobalAlarmEnabled(any()) } just Runs

            viewModel.handleIntent(EventIntent.ToggleGlobalAlarms(false))
            advanceUntilIdle()
            coVerify(exactly = 1) { mockUpdateAppPreferenceUseCase.setGlobalAlarmEnabled(false) }

            viewModel.handleIntent(EventIntent.ToggleGlobalAlarms(true))
            advanceUntilIdle()
            coVerify(exactly = 1) { mockUpdateAppPreferenceUseCase.setGlobalAlarmEnabled(true) }
        }

    @Test
    fun `onUpgradeToProRequest updates showSubscriptionConfirmation to true`() =
        runTest {
            advanceUntilIdle()

            viewModel.uiState.test {
                skipItems(1)

                viewModel.handleIntent(EventIntent.UpgradeToProRequest)
                advanceUntilIdle()

                val updatedState = awaitItem()
                assertTrue(updatedState.showSubscriptionConfirmation)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onDismissUpgradeConfirmation sets showSubscriptionConfirmation and showPurchaseConfirmation to false`() =
        runTest {
            viewModel.handleIntent(EventIntent.UpgradeToProRequest)
            advanceUntilIdle()
            viewModel.handleIntent(EventIntent.DismissUpgradeConfirmation)
            advanceUntilIdle()
            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.showSubscriptionConfirmation)
                assertFalse(state.showPurchaseConfirmation)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onMonthChanged without calendar permission clears events and does not call use case`() =
        runTest {
            every { mockCheckPermissionsUseCase() } returns
                PermissionState(
                    hasCalendarPermission = false,
                    hasPostNotificationsPermission = true,
                    hasExactAlarmPermission = true,
                    hasFullScreenIntentPermission = true,
                    hasLocationPermission = false,
                    hasBackgroundLocationPermission = false,
                )
            val vm = createViewModel()

            clearMocks(mockGetEventsForMonthUseCase, answers = false)
            coEvery { mockGetEventsForMonthUseCase(any()) } returns emptyList()

            vm.handleIntent(EventIntent.ChangeMonth(YearMonth.of(2024, 10)))
            advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.events.isEmpty())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onMonthChanged loads events maps disabled sets and schedules within window`() =
        runTest {
            advanceUntilIdle()

            val now = System.currentTimeMillis()
            val e1 = Event(id = 1, title = "Soon", startTime = now + 30 * 60 * 1000L)
            val e2 = Event(id = 2, title = "Later", startTime = now + 120 * 60 * 1000L)
            val e3 = Event(id = 3, title = "Past", startTime = now - 10 * 60 * 1000L)

            appPreferencesFlow.value =
                defaultAppPreferences().copy(
                    disabledSeriesIds = setOf(e2.id.toString()),
                    disabledEventIds = setOf(e3.uniqueIntentId.toString()),
                )

            coEvery { mockGetEventsForMonthUseCase(any()) } returns listOf(e1, e2, e3)
            clearMocks(mockScheduler, answers = false)

            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.of(2024, 11)))
            advanceUntilIdle()

            verify(atLeast = 1) { mockScheduler.schedule(match { it.id == 1L }, any()) }
            verify(exactly = 0) { mockScheduler.schedule(match { it.id == 2L }, any()) }
            verify(exactly = 0) { mockScheduler.schedule(match { it.id == 3L }, any()) }
        }

    @Test
    fun `onMonthChanged does not schedule when global alarm disabled`() =
        runTest {
            advanceUntilIdle()

            appPreferencesFlow.value = defaultAppPreferences().copy(isGlobalAlarmEnabled = false)
            advanceUntilIdle()

            val now = System.currentTimeMillis()
            val e1 = Event(id = 10, title = "Soon", startTime = now + 10 * 60 * 1000L)
            coEvery { mockGetEventsForMonthUseCase(any()) } returns listOf(e1)
            clearMocks(mockScheduler, answers = false)

            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.of(2025, 1)))
            advanceUntilIdle()

            verify(exactly = 0) { mockScheduler.schedule(any(), any()) }
        }

    @Test
    fun `returnToToday updates selectedDate and currentMonth`() =
        runTest {
            viewModel.handleIntent(EventIntent.SelectDate(LocalDate.of(2000, 1, 1)))
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.ReturnToToday)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(LocalDate.now(), state.selectedDate)
                assertEquals(YearMonth.now(), state.currentMonth)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onVibrateOnlyChanged persists preference`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setVibrateOnly(any()) } just Runs

            viewModel.handleIntent(EventIntent.ToggleVibrateOnly(true))
            advanceUntilIdle()
            coVerify(exactly = 1) { mockUpdateAppPreferenceUseCase.setVibrateOnly(true) }

            viewModel.handleIntent(EventIntent.ToggleVibrateOnly(false))
            advanceUntilIdle()
            coVerify(exactly = 1) { mockUpdateAppPreferenceUseCase.setVibrateOnly(false) }
        }

    @Test
    fun `onEventVibrateToggle calls toggle use case`() =
        runTest {
            val event = Event(id = 1, title = "Test Event", startTime = 0)
            coEvery { mockGetEventsForMonthUseCase(any()) } returns listOf(event)
            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.now()))
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.ToggleEventVibrate(event, enabled = true))
            advanceUntilIdle()

            coVerify(exactly = 1) { mockToggleEventVibrateUseCase(event, true) }
        }

    @Test
    fun `onEventAlarmToggle disables single instance calls toggle use case`() =
        runTest {
            val now = System.currentTimeMillis()
            val event = Event(id = 100, title = "Meeting", startTime = now + 5 * 60 * 1000L, isAlarmEnabled = true)
            coEvery { mockGetEventsForMonthUseCase(any()) } returns listOf(event)
            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.of(2025, 2)))
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.ToggleEventAlarm(event, enabled = false, allOccurrences = false))
            advanceUntilIdle()

            coVerify(exactly = 1) { mockToggleEventAlarmUseCase(event, false, false) }
        }

    @Test
    fun `onEventAlarmToggle enables all occurrences calls toggle use case with allOccurrences true`() =
        runTest {
            val now = System.currentTimeMillis()
            val event =
                Event(
                    id = 200,
                    title = "Standup",
                    startTime = now + 15 * 60 * 1000L,
                    isAlarmEnabled = false,
                    isRecurring = true,
                )
            coEvery { mockGetEventsForMonthUseCase(any()) } returns listOf(event)
            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.of(2025, 3)))
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.ToggleEventAlarm(event, enabled = true, allOccurrences = true))
            advanceUntilIdle()

            coVerify(exactly = 1) { mockToggleEventAlarmUseCase(event, true, true) }
        }

    @Test
    fun `global alarm disabled after events loaded cancels all loaded alarms`() =
        runTest {
            val now = System.currentTimeMillis()
            val e1 = Event(id = 301, title = "A", startTime = now + 5 * 60 * 1000L)
            val e2 = Event(id = 302, title = "B", startTime = now + 6 * 60 * 1000L)
            coEvery { mockGetEventsForMonthUseCase(any()) } returns listOf(e1, e2)
            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.now()))
            advanceUntilIdle()

            appPreferencesFlow.value = defaultAppPreferences().copy(isGlobalAlarmEnabled = false)
            advanceUntilIdle()

            verify { mockScheduler.cancel(match { it.id == 301L }) }
            verify { mockScheduler.cancel(match { it.id == 302L }) }
        }

    @Test
    fun `onAlarmOffsetChanged calls use case`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setAlarmOffsetMinutes(any()) } just Runs
            viewModel.handleIntent(EventIntent.UpdateAlarmOffset(AlarmOffset.FIFTEEN_MINUTES))
            advanceUntilIdle()
            coVerify { mockUpdateAppPreferenceUseCase.setAlarmOffsetMinutes(15L) }
        }

    @Test
    fun `loadAvailableCalendars updates UI state`() =
        runTest {
            val mockCalendars = listOf(DeviceCalendar(1, "Calendar 1", "Account 1"))
            coEvery { mockGetAvailableCalendarsUseCase() } returns mockCalendars

            viewModel.handleIntent(EventIntent.LoadCalendars)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(mockCalendars, state.availableCalendars)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onCalendarFilterToggle updates preferences`() =
        runTest {
            val mockCalendars =
                listOf(
                    DeviceCalendar(1, "Calendar 1", "Account 1"),
                    DeviceCalendar(2, "Calendar 2", "Account 2"),
                )
            coEvery { mockGetAvailableCalendarsUseCase() } returns mockCalendars
            appPreferencesFlow.value = defaultAppPreferences().copy(enabledCalendarIds = setOf("1"))

            viewModel.handleIntent(EventIntent.LoadCalendars)
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.ToggleCalendarFilter(2L, true))
            advanceUntilIdle()

            coVerify { mockUpdateAppPreferenceUseCase.setEnabledCalendarIds(any()) }
        }

    @Test
    fun `clearCalendarFilter calls use case with empty set`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setEnabledCalendarIds(emptySet()) } just Runs
            viewModel.handleIntent(EventIntent.ClearCalendarFilter)
            advanceUntilIdle()
            coVerify { mockUpdateAppPreferenceUseCase.setEnabledCalendarIds(emptySet()) }
        }

    @Test
    fun `generateDailyBriefing does nothing if user is not AI user`() =
        runTest {
            isAiUserFlow.value = false
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.GenerateDailyBriefing("instruction"))
            advanceUntilIdle()

            coVerify(exactly = 0) { mockGenerateDailyBriefingUseCase(any(), any()) }
        }

    @Test
    fun `generateDailyBriefing calls usecase if user is AI user and has events today`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            val today = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val mockEvents = listOf(Event(1, "Today Event", today))
            val yearMonth = YearMonth.now()
            coEvery { mockGetEventsForMonthUseCase(yearMonth) } returns mockEvents

            viewModel.handleIntent(EventIntent.ChangeMonth(yearMonth))
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.GenerateDailyBriefing("instruction"))
            advanceUntilIdle()

            coVerify(exactly = 1) { mockGenerateDailyBriefingUseCase(any(), "instruction") }
        }

    @Test
    fun `generateDailyBriefing calls usecase even if user has no events today`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            coEvery { mockGetEventsForMonthUseCase(any()) } returns emptyList()

            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.now()))
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.GenerateDailyBriefing("instruction"))
            advanceUntilIdle()

            coVerify(exactly = 1) { mockGenerateDailyBriefingUseCase(emptyList(), "instruction") }
        }

    @Test
    fun `askAi updates UI state and calls usecase`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            coEvery { mockAskAiAboutScheduleUseCase(any(), any(), any()) } returns "AI Response"

            viewModel.handleIntent(EventIntent.AskAi("What's next?", "instruction"))
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("AI Response", state.aiResponse)
                assertFalse(state.isAskingAi)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `askAi with JSON in markdown blocks opens create event dialog`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isAiUser)

            val jsonResponse =
                """
                ```json
                {
                  "title": "Dentista",
                  "startTime": 1710101000000,
                  "endTime": 1710104600000,
                  "isAllDay": false
                }
                ```
                """.trimIndent()

            coEvery {
                mockAskAiAboutScheduleUseCase(any(), any(), any())
            } returns jsonResponse

            viewModel.handleIntent(EventIntent.AskAi("Marcar dentista", "instrucao"))
            advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertFalse(finalState.isAskingAi)
            assertTrue(finalState.showCreateEventDialog)
            assertEquals("Dentista", finalState.voiceEventData?.title)
        }

    @Test
    fun `clearAiResponse resets UI state`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()
            coEvery { mockAskAiAboutScheduleUseCase(any(), any(), any()) } returns "Some response"
            viewModel.handleIntent(EventIntent.AskAi("Q", "I"))
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.ClearAiResponse)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.aiResponse)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `showAiSuggestionsDialog and dismissAiSuggestionsDialog toggle UI state`() =
        runTest {
            viewModel.handleIntent(EventIntent.ShowAiSuggestionsDialog)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showAiSuggestionsDialog)

            viewModel.handleIntent(EventIntent.DismissAiSuggestionsDialog)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showAiSuggestionsDialog)
        }

    @Test
    fun `rateLater hides dialog without marking as completed`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                viewModel.handleIntent(EventIntent.RateLater)
                advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.showRatingBottomSheet)
                coVerify(exactly = 0) { mockUpdateAppPreferenceUseCase.setRatingCompleted(true) }
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `rateNever marks rating as completed and hides dialog`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setRatingCompleted(true) } just Runs

            viewModel.uiState.test {
                skipItems(1)

                viewModel.handleIntent(EventIntent.RateNever)
                advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.showRatingBottomSheet)
                coVerify(exactly = 1) { mockUpdateAppPreferenceUseCase.setRatingCompleted(true) }
                cancelAndConsumeRemainingEvents()
            }
        }
}
