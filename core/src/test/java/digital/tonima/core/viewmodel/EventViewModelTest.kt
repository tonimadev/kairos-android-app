package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.AIToolResult
import digital.tonima.core.ai.ActionRegistry
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.analytics.EventAnalytics
import digital.tonima.core.database.dao.ChatHistoryDao
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.review.ReviewManager
import digital.tonima.core.usecases.AppPreferences
import digital.tonima.core.usecases.AskAiAgentUseCase
import digital.tonima.core.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.usecases.CancelEventAlarmUseCase
import digital.tonima.core.usecases.CheckPermissionsUseCase
import digital.tonima.core.usecases.ClearChatHistoryUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.usecases.GetChatHistoryUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.usecases.InsertChatMessageUseCase
import digital.tonima.core.usecases.LogEventUseCase
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.ObserveChatHistoryUseCase
import digital.tonima.core.usecases.ObserveDailyBriefingUseCase
import digital.tonima.core.usecases.ObserveRingerModeUseCase
import digital.tonima.core.usecases.PermissionState
import digital.tonima.core.usecases.ProcessAiResponseUseCase
import digital.tonima.core.usecases.ScheduleEventAlarmUseCase
import digital.tonima.core.usecases.SpeakTextUseCase
import digital.tonima.core.usecases.ToggleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventVibrateUseCase
import digital.tonima.core.usecases.ToggleFocusModeUseCase
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import digital.tonima.core.usecases.UpdateWidgetUseCase
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
    private val mockAskAiAgentUseCase: AskAiAgentUseCase = mockk(relaxed = true)
    private val mockActionRegistry: ActionRegistry = mockk(relaxed = true)
    private val mockCreateEventUseCase: CreateEventUseCase = mockk(relaxed = true)
    private val mockCalculateDepartureTimeUseCase: CalculateDepartureTimeUseCase = mockk(relaxed = true)
    private val mockCheckPermissionsUseCase: CheckPermissionsUseCase = mockk(relaxed = true)
    private val mockToggleFocusModeUseCase: ToggleFocusModeUseCase = mockk(relaxed = true)
    private val mockTtsHelper: TextToSpeechHelper = mockk(relaxed = true)
    private val mockWidgetUpdater: WidgetUpdater = mockk(relaxed = true)
    private val mockReviewManager: ReviewManager = mockk(relaxed = true)
    private val mockEventAnalytics: EventAnalytics = mockk(relaxed = true)
    private val mockChatHistoryDao: ChatHistoryDao = mockk(relaxed = true)

    private val mockScheduleEventAlarmUseCase: ScheduleEventAlarmUseCase = mockk(relaxed = true)
    private val mockCancelEventAlarmUseCase: CancelEventAlarmUseCase = mockk(relaxed = true)
    private val mockSpeakTextUseCase: SpeakTextUseCase = mockk(relaxed = true)
    private val mockUpdateWidgetUseCase: UpdateWidgetUseCase = mockk(relaxed = true)
    private val mockProcessAiResponseUseCase: ProcessAiResponseUseCase = mockk(relaxed = true)
    private val mockObserveChatHistoryUseCase: ObserveChatHistoryUseCase = mockk(relaxed = true)
    private val mockGetChatHistoryUseCase: GetChatHistoryUseCase = mockk(relaxed = true)
    private val mockInsertChatMessageUseCase: InsertChatMessageUseCase = mockk(relaxed = true)
    private val mockClearChatHistoryUseCase: ClearChatHistoryUseCase = mockk(relaxed = true)
    private val mockLogEventUseCase: LogEventUseCase = mockk(relaxed = true)

    private val appPreferencesFlow = MutableStateFlow(defaultAppPreferences())
    private val ringerModeFlow = MutableStateFlow(AudioWarningState.NORMAL)
    private val dailyBriefingFlow = MutableStateFlow<String?>(null)
    private val isProUserFlow = MutableStateFlow(false)
    private val isAiUserFlow = MutableStateFlow(false)
    private val fakeChatHistory = mutableListOf<digital.tonima.core.ai.model.ChatMessage>()

    private val mockLocationRepository: digital.tonima.core.repository.LocationRepository = mockk(relaxed = true)
    private val mockWeatherRepository: digital.tonima.core.repository.WeatherRepository = mockk(relaxed = true)

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

        every { mockObserveChatHistoryUseCase() } answers { MutableStateFlow(fakeChatHistory.toList()) }
        coEvery { mockGetChatHistoryUseCase() } answers { fakeChatHistory.toList() }
        coEvery { mockInsertChatMessageUseCase(any()) } answers {
            fakeChatHistory.add(firstArg())
            1L
        }
        coEvery { mockClearChatHistoryUseCase() } answers {
            fakeChatHistory.clear()
            Unit
            1
        }

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
            generateDailyBriefingUseCase = mockGenerateDailyBriefingUseCase,
            askAiAgentUseCase = mockAskAiAgentUseCase,
            calculateDepartureTimeUseCase = mockCalculateDepartureTimeUseCase,
            observeDailyBriefingUseCase = mockObserveDailyBriefingUseCase,
            getRegisteredAiToolsUseCase = io.mockk.mockk(relaxed = true),
            processAiResponseUseCase = mockProcessAiResponseUseCase,
            speakTextUseCase = mockSpeakTextUseCase,
            updateWidgetUseCase = mockUpdateWidgetUseCase,
            observeChatHistoryUseCase = mockObserveChatHistoryUseCase,
            getChatHistoryUseCase = mockGetChatHistoryUseCase,
            insertChatMessageUseCase = mockInsertChatMessageUseCase,
            clearChatHistoryUseCase = mockClearChatHistoryUseCase,
            logEventUseCase = mockLogEventUseCase,
            observeRingerModeUseCase = mockObserveRingerModeUseCase,
            checkPermissionsUseCase = mockCheckPermissionsUseCase,
            toggleFocusModeUseCase = mockToggleFocusModeUseCase,
            reviewManager = mockReviewManager,
            fetchMeetingTranscriptUseCase = io.mockk.mockk(relaxed = true),
            isGoogleSignedIn = io.mockk.mockk(relaxed = true),
            getGoogleSignInIntent = io.mockk.mockk(relaxed = true),
            handleGoogleSignInResultUseCase = io.mockk.mockk(relaxed = true),
            signOutFromGoogle = io.mockk.mockk(relaxed = true),
            getCurrentLocationUseCase = io.mockk.mockk(relaxed = true),
            getWeatherUseCase = io.mockk.mockk(relaxed = true),
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
    fun `onUpgradeToProRequest updates showPurchaseConfirmation to true`() =
        runTest {
            advanceUntilIdle()

            viewModel.uiState.test {
                skipItems(1)

                viewModel.handleIntent(EventIntent.UpgradeToProRequest)
                advanceUntilIdle()

                val updatedState = awaitItem()
                assertTrue(updatedState.showPurchaseConfirmation)
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

            viewModel.handleIntent(EventIntent.ChangeMonth(YearMonth.of(2024, 11)))
            advanceUntilIdle()
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

            verify { mockCancelEventAlarmUseCase(match { it.id == 301L }) }
            verify { mockCancelEventAlarmUseCase(match { it.id == 302L }) }
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

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.Text("AI Response")

            viewModel.handleIntent(EventIntent.AskAi("What's next?", "instruction"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("AI Response", state.aiResponse)
            assertFalse(state.isAskingAi)
        }

    @Test
    fun `askAi maintains chat history`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            val response1 = AIAgentResponse.Text("Response 1")
            val response2 = AIAgentResponse.Text("Response 2")

            coEvery { mockAskAiAgentUseCase(any(), "Q1", any(), any(), emptyList()) } returns response1
            coEvery {
                mockAskAiAgentUseCase(
                    any(),
                    "Q2",
                    any(),
                    any(),
                    match {
                        it.size == 2 &&
                            (it[0] as? ChatMessage.Text)?.content == "Q1" &&
                            (it[1] as? ChatMessage.Text)?.content == "Response 1"
                    },
                )
            } returns response2

            // First interaction
            viewModel.handleIntent(EventIntent.AskAi("Q1", "I"))
            advanceUntilIdle()

            assertEquals("Response 1", viewModel.uiState.value.aiResponse)
            assertEquals(2, fakeChatHistory.size)

            // Second interaction (continuation)
            viewModel.handleIntent(EventIntent.AskAi("Q2", "I"))
            advanceUntilIdle()

            assertEquals("Response 2", viewModel.uiState.value.aiResponse)
            assertEquals(4, fakeChatHistory.size)
            assertEquals("Q2", (fakeChatHistory[2] as digital.tonima.core.ai.model.ChatMessage.Text).content)
            assertEquals("Response 2", (fakeChatHistory[3] as digital.tonima.core.ai.model.ChatMessage.Text).content)

            // Clear response should clear history
            viewModel.handleIntent(EventIntent.ClearAiResponse)
            advanceUntilIdle()
            assertTrue(fakeChatHistory.isEmpty())
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
                mockAskAiAgentUseCase(any(), any(), any(), any(), any())
            } returns AIAgentResponse.Text(jsonResponse)

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
            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.Text("Some response")
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

    // ── AI Agent tests ──────────────────────────────────────────────────

    @Test
    fun `onAIFunctionCalled with SAFE tool dispatches intent immediately`() =
        runTest {
            advanceUntilIdle()

            val safeTool = mockk<AITool>(relaxed = true)
            every { safeTool.riskLevel } returns RiskLevel.SAFE
            every { safeTool.name } returns "search_events"

            val searchIntent = EventIntent.SearchQueryChanged("meeting")
            every {
                mockProcessAiResponseUseCase("search_events", any())
            } returns AIToolResult.Success(safeTool, searchIntent)

            viewModel.onAIFunctionCalled("search_events", mapOf("query" to "meeting"))
            advanceUntilIdle()

            assertEquals("meeting", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `onAIFunctionCalled with MODERATE tool dispatches intent and emits snackbar`() =
        runTest {
            advanceUntilIdle()

            val moderateTool = mockk<AITool>(relaxed = true)
            every { moderateTool.riskLevel } returns RiskLevel.MODERATE
            every { moderateTool.name } returns "toggle_global_alarms"

            every {
                mockProcessAiResponseUseCase("toggle_global_alarms", any())
            } returns
                AIToolResult.Success(
                    moderateTool,
                    EventIntent.ToggleGlobalAlarms(false),
                )

            viewModel.sideEffect.test {
                viewModel.onAIFunctionCalled("toggle_global_alarms", mapOf("enabled" to false))
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is EventSideEffect.ShowSnackbar)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onAIFunctionCalled with CRITICAL tool saves pending action and emits confirmation`() =
        runTest {
            advanceUntilIdle()

            val criticalTool = mockk<AITool>(relaxed = true)
            every { criticalTool.riskLevel } returns RiskLevel.CRITICAL
            every { criticalTool.name } returns "create_event"

            val createIntent =
                EventIntent.CreateEvent(
                    calendarId = 1L,
                    title = "Dentista",
                    description = null,
                    location = null,
                    startTime = 1000L,
                    endTime = 2000L,
                    isAllDay = false,
                )

            every {
                mockProcessAiResponseUseCase("create_event", any())
            } returns AIToolResult.Success(criticalTool, createIntent)

            viewModel.sideEffect.test {
                viewModel.onAIFunctionCalled("create_event", emptyMap())
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is EventSideEffect.RequireUserConfirmation)
                cancelAndConsumeRemainingEvents()
            }

            assertEquals(createIntent, viewModel.uiState.value.pendingAIAction)
        }

    @Test
    fun `ApprovePendingAction executes saved intent and clears pending`() =
        runTest {
            advanceUntilIdle()

            val criticalTool = mockk<AITool>(relaxed = true)
            every { criticalTool.riskLevel } returns RiskLevel.CRITICAL
            every { criticalTool.name } returns "create_event"

            val searchIntent = EventIntent.SearchQueryChanged("approved")
            every {
                mockProcessAiResponseUseCase("create_event", any())
            } returns AIToolResult.Success(criticalTool, searchIntent)

            viewModel.onAIFunctionCalled("create_event", emptyMap())
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.pendingAIAction)

            viewModel.handleIntent(EventIntent.ApprovePendingAction)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingAIAction)
            assertEquals("approved", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `RejectPendingAction clears pending without executing`() =
        runTest {
            advanceUntilIdle()

            val criticalTool = mockk<AITool>(relaxed = true)
            every { criticalTool.riskLevel } returns RiskLevel.CRITICAL
            every { criticalTool.name } returns "create_event"

            every {
                mockProcessAiResponseUseCase("create_event", any())
            } returns
                AIToolResult.Success(
                    criticalTool,
                    EventIntent.SearchQueryChanged("should_not_appear"),
                )

            viewModel.onAIFunctionCalled("create_event", emptyMap())
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.RejectPendingAction)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingAIAction)
            assertEquals("", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `onAIFunctionCalled with unknown tool emits AIToolError`() =
        runTest {
            advanceUntilIdle()

            every {
                mockProcessAiResponseUseCase("unknown", any())
            } returns AIToolResult.ToolNotFound("unknown")

            viewModel.sideEffect.test {
                viewModel.onAIFunctionCalled("unknown", emptyMap())
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is EventSideEffect.AIToolError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onAIFunctionCalled with invalid args emits AIToolError`() =
        runTest {
            advanceUntilIdle()

            val args = mapOf<String, Any?>("bad" to "data")
            every {
                mockProcessAiResponseUseCase("create_event", args)
            } returns AIToolResult.InvalidArguments("create_event", args)

            viewModel.sideEffect.test {
                viewModel.onAIFunctionCalled("create_event", args)
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is EventSideEffect.AIToolError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `askAi with FunctionCall response calls onAIFunctionCalled`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            coEvery {
                mockAskAiAgentUseCase(any(), any(), any(), any())
            } returns AIAgentResponse.FunctionCall("search_events", mapOf("query" to "gym"))

            val safeTool = mockk<AITool>(relaxed = true)
            every { safeTool.riskLevel } returns RiskLevel.SAFE
            every { safeTool.name } returns "search_events"
            every {
                mockProcessAiResponseUseCase("search_events", any())
            } returns AIToolResult.Success(safeTool, EventIntent.SearchQueryChanged("gym"))

            viewModel.handleIntent(EventIntent.AskAi("find gym", "en"))
            advanceUntilIdle()

            assertEquals("gym", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `askAi with Empty response does not change state`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            coEvery {
                mockAskAiAgentUseCase(any(), any(), any(), any())
            } returns AIAgentResponse.Empty

            viewModel.handleIntent(EventIntent.AskAi("hello", "en"))
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.aiResponse)
            assertFalse(viewModel.uiState.value.isAskingAi)
        }

    @Test
    fun `ApprovePendingAction for CreateEvent opens create event dialog`() =
        runTest {
            advanceUntilIdle()

            val criticalTool = mockk<AITool>(relaxed = true)
            every { criticalTool.riskLevel } returns RiskLevel.CRITICAL
            every { criticalTool.name } returns "create_event"

            val createIntent =
                EventIntent.CreateEvent(
                    calendarId = 1,
                    title = "Meeting",
                    description = "Desc",
                    location = "Office",
                    startTime = 1000L,
                    endTime = 2000L,
                    isAllDay = false,
                )

            every {
                mockProcessAiResponseUseCase("create_event", any())
            } returns AIToolResult.Success(criticalTool, createIntent)

            viewModel.onAIFunctionCalled("create_event", emptyMap())
            advanceUntilIdle()

            viewModel.handleIntent(EventIntent.ApprovePendingAction)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.pendingAIAction)
            assertTrue(state.showCreateEventDialog)
            assertEquals("Meeting", state.voiceEventData?.title)
            assertEquals("Office", state.voiceEventData?.location)
            assertEquals(1000L, state.voiceEventData?.startTime)
        }

    @Test
    fun `CreateEvent intent calls use case and shows success snackbar`() =
        runTest {
            advanceUntilIdle()

            coEvery {
                mockCreateEventUseCase(any(), any(), any(), any(), any(), any(), any())
            } returns 123L

            viewModel.sideEffect.test {
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

                val effect = awaitItem()
                assertTrue(effect is EventSideEffect.ShowSnackbar)
                val snackbarEffect = effect as EventSideEffect.ShowSnackbar
                // We check the message is not null, the specific text depends on translation
                assertNotNull(snackbarEffect.message)

                cancelAndConsumeRemainingEvents()
            }

            coVerify {
                mockCreateEventUseCase(1, "New Event", "Desc", "Loc", 1000L, 2000L, false)
            }
            assertFalse(viewModel.uiState.value.showCreateEventDialog)
        }

    @Test
    fun `ToggleFocusMode intent enables DND when permission granted`() =
        runTest {
            every { mockToggleFocusModeUseCase(true) } returns Result.success(Unit)

            viewModel.handleIntent(EventIntent.ToggleFocusMode(true))
            advanceUntilIdle()

            verify { mockToggleFocusModeUseCase(true) }
        }

    @Test
    fun `ToggleFocusMode intent emits error when permission not granted`() =
        runTest {
            every { mockToggleFocusModeUseCase(true) } returns Result.failure(Exception("No access"))

            viewModel.sideEffect.test {
                viewModel.handleIntent(EventIntent.ToggleFocusMode(true))
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is EventSideEffect.AIToolError)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `NotifyRunningLate intent emits snackbar with message`() =
        runTest {
            viewModel.sideEffect.test {
                viewModel.handleIntent(EventIntent.NotifyRunningLate("event1", "I'm late"))
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is EventSideEffect.ShowSnackbar)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `SkipExactAlarmPermission sets skipped flag and updates UI state to hasExactAlarmPermission true`() =
        runTest {
            advanceUntilIdle()

            // Assume initial state has no exact alarm permission
            every { mockCheckPermissionsUseCase() } returns
                PermissionState(
                    hasCalendarPermission = true,
                    hasPostNotificationsPermission = true,
                    hasExactAlarmPermission = false,
                    hasFullScreenIntentPermission = true,
                    hasLocationPermission = false,
                    hasBackgroundLocationPermission = false,
                )

            viewModel.handleIntent(EventIntent.CheckPermissions)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.hasExactAlarmPermission)

            // Skip permission
            coEvery { mockUpdateAppPreferenceUseCase.setExactAlarmPermissionSkipped(true) } just Runs

            viewModel.handleIntent(EventIntent.SkipExactAlarmPermission)
            advanceUntilIdle()

            coVerify { mockUpdateAppPreferenceUseCase.setExactAlarmPermissionSkipped(true) }
        }

    @Test
    fun `SkipFullScreenIntentPermission calls update preference use case`() =
        runTest {
            advanceUntilIdle()

            // Skip permission
            coEvery { mockUpdateAppPreferenceUseCase.setFullScreenIntentPermissionSkipped(true) } just Runs

            viewModel.handleIntent(EventIntent.SkipFullScreenIntentPermission)
            advanceUntilIdle()

            coVerify { mockUpdateAppPreferenceUseCase.setFullScreenIntentPermissionSkipped(true) }
        }
}
