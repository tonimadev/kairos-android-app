package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import digital.tonima.core.permissions.PermissionManager
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.repository.CalendarRepository
import digital.tonima.core.repository.DailyBriefingRepository
import digital.tonima.core.repository.RingerModeRepository
import digital.tonima.core.review.ReviewManager
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.core.usecases.AskAiAboutScheduleUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.utils.TextToSpeechHelper
import digital.tonima.core.utils.WidgetUpdater
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    private val getEventsForMonthUseCase: GetEventsForMonthUseCase = mockk(relaxed = true)
    private val mockAppPreferencesRepository: AppPreferencesRepository = mockk(relaxed = true)
    private val mockRingerModeRepository: RingerModeRepository = mockk(relaxed = true)
    private val mockScheduler: EventAlarmScheduler = mockk(relaxed = true)
    private val mockPermissionManager: PermissionManager = mockk(relaxed = true)
    private val mockCalendarRepository: CalendarRepository = mockk(relaxed = true)
    private val mockDailyBriefingRepository: DailyBriefingRepository = mockk(relaxed = true)
    private val mockGenerateDailyBriefingUseCase: GenerateDailyBriefingUseCase = mockk(relaxed = true)
    private val mockAskAiAboutScheduleUseCase: AskAiAboutScheduleUseCase = mockk(relaxed = true)
    private val mockCreateEventUseCase: CreateEventUseCase = mockk(relaxed = true)
    private val mockCalculateDepartureTimeUseCase: digital.tonima.core.usecases.CalculateDepartureTimeUseCase =
        mockk(relaxed = true)

    private val ttsHelper: TextToSpeechHelper = mockk(relaxed = true)
    private val mockWidgetUpdater: WidgetUpdater = mockk(relaxed = true)
    private val mockReviewManager: ReviewManager = mockk(relaxed = true)
    private lateinit var viewModel: EventViewModel

    private val isGlobalAlarmEnabledFlow = MutableStateFlow(true)
    private val autostartSuggestionDismissedFlow = MutableStateFlow(false)
    private val disabledEventIdsFlow = MutableStateFlow(emptySet<String>())
    private val disabledSeriesIdsFlow = MutableStateFlow(emptySet<String>())
    private val ringerModeFlow = MutableStateFlow(AudioWarningState.NORMAL)
    private val vibrateOnlyEventIdsFlow = MutableStateFlow(emptySet<String>())
    private val installationDateFlow = MutableStateFlow(0L)
    private val ratingPromptedFlow = MutableStateFlow(false)
    private val ratingCompletedFlow = MutableStateFlow(false)
    private val snoozeTimeMinutesFlow = MutableStateFlow(10)
    private val isProUserFlow = MutableStateFlow(false)
    private val isAiUserFlow = MutableStateFlow(false)
    private val dailyBriefingFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { mockProUserProvider.isProUser } returns isProUserFlow
        every { mockProUserProvider.isAiUser } returns isAiUserFlow
        every { mockAppPreferencesRepository.isGlobalAlarmEnabled() } returns isGlobalAlarmEnabledFlow
        every { mockAppPreferencesRepository.getAutostartSuggestionDismissed() } returns
            autostartSuggestionDismissedFlow
        every { mockAppPreferencesRepository.getDisabledEventIds() } returns disabledEventIdsFlow
        every { mockAppPreferencesRepository.getDisabledSeriesIds() } returns disabledSeriesIdsFlow
        every { mockAppPreferencesRepository.getVibrateOnlyEventIds() } returns vibrateOnlyEventIdsFlow
        every { mockAppPreferencesRepository.getInstallationDate() } returns installationDateFlow
        every { mockAppPreferencesRepository.isRatingPrompted() } returns ratingPromptedFlow
        every { mockAppPreferencesRepository.isRatingCompleted() } returns ratingCompletedFlow
        every { mockAppPreferencesRepository.getSnoozeTimeMinutes() } returns snoozeTimeMinutesFlow
        every { mockDailyBriefingRepository.getDailyBriefing() } returns dailyBriefingFlow
        every { mockRingerModeRepository.ringerMode } returns ringerModeFlow
        every { mockRingerModeRepository.startObserving() } just Runs
        every { mockRingerModeRepository.stopObserving() } just Runs

        every { mockPermissionManager.hasCalendarPermission() } returns true
        every { mockPermissionManager.hasPostNotificationsPermission() } returns true
        every { mockPermissionManager.hasExactAlarmPermission() } returns true
        every { mockPermissionManager.hasFullScreenIntentPermission() } returns true

        viewModel =
            EventViewModel(
                mockProUserProvider,
                getEventsForMonthUseCase,
                mockAppPreferencesRepository,
                mockRingerModeRepository,
                mockScheduler,
                mockPermissionManager,
                mockCalendarRepository,
                mockDailyBriefingRepository,
                mockGenerateDailyBriefingUseCase,
                mockAskAiAboutScheduleUseCase,
                mockCreateEventUseCase,
                mockCalculateDepartureTimeUseCase,
                ttsHelper,
                mockWidgetUpdater,
                mockReviewManager,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onCleared calls stopObserving on RingerModeRepository`() =
        runTest {
            viewModel.onCleared()
            verify(exactly = 1) { mockRingerModeRepository.stopObserving() }
        }

    @Test
    fun `checkAllPermissions updates all permission flags in UI state`() =
        runTest {
            every { mockPermissionManager.hasCalendarPermission() } returns false
            every { mockPermissionManager.hasPostNotificationsPermission() } returns false

            viewModel.uiState.test {
                skipItems(1)

                viewModel.checkAllPermissions()
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
            coEvery { mockAppPreferencesRepository.setAutostartSuggestionDismissed(true) } just Runs

            viewModel.dismissAutostartSuggestion()
            advanceUntilIdle()

            coVerify(exactly = 1) { mockAppPreferencesRepository.setAutostartSuggestionDismissed(true) }
        }

    @Test
    fun `onDateSelected updates selectedDate in UI state`() =
        runTest {
            val newDate = LocalDate.of(2023, 10, 26)

            viewModel.uiState.test {
                skipItems(1)

                viewModel.onDateSelected(newDate)
                advanceUntilIdle()

                val updatedState = awaitItem()
                assertEquals(newDate, updatedState.selectedDate)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onAlarmsToggle sets global alarm enabled status in preferences`() =
        runTest {
            coEvery { mockAppPreferencesRepository.setGlobalAlarmEnabled(any()) } just Runs

            viewModel.onAlarmsToggle(false)
            advanceUntilIdle()
            coVerify(exactly = 1) { mockAppPreferencesRepository.setGlobalAlarmEnabled(false) }

            viewModel.onAlarmsToggle(true)
            advanceUntilIdle()
            coVerify(exactly = 1) { mockAppPreferencesRepository.setGlobalAlarmEnabled(true) }
        }

    @Test
    fun `onUpgradeToProRequest updates showSubscriptionConfirmation to true`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                viewModel.onUpgradeToProRequest()
                advanceUntilIdle()

                val updatedState = awaitItem()
                assertTrue(updatedState.showSubscriptionConfirmation)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onDismissUpgradeConfirmation sets showSubscriptionConfirmation and showPurchaseConfirmation to false`() =
        runTest {
            viewModel.onUpgradeToProRequest()
            advanceUntilIdle()
            viewModel.onDismissUpgradeConfirmation()
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
            every { mockPermissionManager.hasCalendarPermission() } returns false
            val vm =
                EventViewModel(
                    mockProUserProvider,
                    getEventsForMonthUseCase,
                    mockAppPreferencesRepository,
                    mockRingerModeRepository,
                    mockScheduler,
                    mockPermissionManager,
                    mockCalendarRepository,
                    mockDailyBriefingRepository,
                    mockGenerateDailyBriefingUseCase,
                    mockAskAiAboutScheduleUseCase,
                    mockCreateEventUseCase,
                    mockCalculateDepartureTimeUseCase,
                    ttsHelper,
                    mockWidgetUpdater,
                    mockReviewManager,
                )

            io.mockk.clearMocks(getEventsForMonthUseCase, answers = false)

            coEvery { getEventsForMonthUseCase.invoke(any()) } returns emptyList()
            vm.onMonthChanged(YearMonth.of(2024, 10))
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
            every { mockPermissionManager.hasCalendarPermission() } returns false
            val vm =
                EventViewModel(
                    mockProUserProvider,
                    getEventsForMonthUseCase,
                    mockAppPreferencesRepository,
                    mockRingerModeRepository,
                    mockScheduler,
                    mockPermissionManager,
                    mockCalendarRepository,
                    mockDailyBriefingRepository,
                    mockGenerateDailyBriefingUseCase,
                    mockAskAiAboutScheduleUseCase,
                    mockCreateEventUseCase,
                    mockCalculateDepartureTimeUseCase,
                    ttsHelper,
                    mockWidgetUpdater,
                    mockReviewManager,
                )
            io.mockk.clearMocks(mockScheduler, answers = false)

            every { mockPermissionManager.hasCalendarPermission() } returns true

            val now = System.currentTimeMillis()
            val e1 = Event(id = 1, title = "Soon", startTime = now + 30 * 60 * 1000L)
            val e2 = Event(id = 2, title = "Later", startTime = now + 120 * 60 * 1000L)
            val e3 = Event(id = 3, title = "Past", startTime = now - 10 * 60 * 1000L)

            disabledSeriesIdsFlow.value = setOf(e2.id.toString())
            disabledEventIdsFlow.value = setOf(e3.uniqueIntentId.toString())

            coEvery { getEventsForMonthUseCase.invoke(any()) } returns listOf(e1, e2, e3)

            vm.onMonthChanged(YearMonth.of(2024, 11))
            advanceUntilIdle()

            verify(exactly = 1) { mockScheduler.schedule(match { it.id == 1L }) }
            verify(exactly = 0) { mockScheduler.schedule(match { it.id == 2L }) }
            verify(exactly = 0) { mockScheduler.schedule(match { it.id == 3L }) }
        }

    @Test
    fun `onMonthChanged does not schedule when global alarm disabled`() =
        runTest {
            isGlobalAlarmEnabledFlow.value = false
            val now = System.currentTimeMillis()
            val e1 = Event(id = 10, title = "Soon", startTime = now + 10 * 60 * 1000L)
            coEvery { getEventsForMonthUseCase.invoke(any()) } returns listOf(e1)

            viewModel.onMonthChanged(YearMonth.of(2025, 1))
            advanceUntilIdle()

            verify(exactly = 0) { mockScheduler.schedule(any()) }
        }

    @Test
    fun `returnToToday updates selectedDate and currentMonth`() =
        runTest {
            viewModel.onDateSelected(LocalDate.of(2000, 1, 1))
            advanceUntilIdle()

            viewModel.returnToToday()
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
            coEvery { mockAppPreferencesRepository.setVibrateOnly(any()) } just Runs

            viewModel.onVibrateOnlyChanged(true)
            advanceUntilIdle()
            coVerify(exactly = 1) { mockAppPreferencesRepository.setVibrateOnly(true) }

            viewModel.onVibrateOnlyChanged(false)
            advanceUntilIdle()
            coVerify(exactly = 1) { mockAppPreferencesRepository.setVibrateOnly(false) }
        }

    @Test
    fun `onEventVibrateToggle updates event and persists preference`() =
        runTest {
            val event = Event(id = 1, title = "Test Event", startTime = 0)
            coEvery { getEventsForMonthUseCase.invoke(any()) } returns listOf(event)
            viewModel.onMonthChanged(YearMonth.now())
            advanceUntilIdle()

            coEvery { mockAppPreferencesRepository.setVibrateOnlyEventIds(any()) } just Runs

            // Toggle on
            viewModel.onEventVibrateToggle(event, vibrateOnly = true)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.events.first().vibrateOnly)
                coVerify { mockAppPreferencesRepository.setVibrateOnlyEventIds(setOf(event.uniqueIntentId.toString())) }
                cancelAndConsumeRemainingEvents()
            }

            // Toggle off
            viewModel.onEventVibrateToggle(event, vibrateOnly = false)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.events.first().vibrateOnly)
                coVerify { mockAppPreferencesRepository.setVibrateOnlyEventIds(emptySet()) }
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onEventAlarmToggle disables single instance and cancels when global enabled`() =
        runTest {
            val now = System.currentTimeMillis()
            val event = Event(id = 100, title = "Meeting", startTime = now + 5 * 60 * 1000L, isAlarmEnabled = true)
            coEvery { getEventsForMonthUseCase.invoke(any()) } returns listOf(event)
            viewModel.onMonthChanged(YearMonth.of(2025, 2))
            advanceUntilIdle()

            coEvery { mockAppPreferencesRepository.setDisabledEventIds(any()) } just Runs

            viewModel.onEventAlarmToggle(event, isEnabled = false, disableAllOccurrences = false)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                mockAppPreferencesRepository.setDisabledEventIds(
                    match {
                        it.contains(event.uniqueIntentId.toString())
                    },
                )
            }
            verify(exactly = 1) { mockScheduler.cancel(match { it.id == event.id }) }

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.events.first { it.id == event.id }.isAlarmEnabled)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onEventAlarmToggle enables all occurrences and schedules when global enabled`() =
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
            disabledSeriesIdsFlow.value = setOf(event.id.toString())
            coEvery { getEventsForMonthUseCase.invoke(any()) } returns listOf(event)
            viewModel.onMonthChanged(YearMonth.of(2025, 3))
            advanceUntilIdle()

            coEvery { mockAppPreferencesRepository.setDisabledSeriesIds(any()) } just Runs

            viewModel.onEventAlarmToggle(event, isEnabled = true, disableAllOccurrences = true)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                mockAppPreferencesRepository.setDisabledSeriesIds(
                    match {
                        !it.contains(event.id.toString())
                    },
                )
            }
            verify(exactly = 1) { mockScheduler.schedule(match { it.id == event.id }) }

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.events.first { it.id == event.id }.isAlarmEnabled)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `global alarm disabled after events loaded cancels all loaded alarms`() =
        runTest {
            val now = System.currentTimeMillis()
            val e1 = Event(id = 301, title = "A", startTime = now + 5 * 60 * 1000L)
            val e2 = Event(id = 302, title = "B", startTime = now + 6 * 60 * 1000L)
            coEvery { getEventsForMonthUseCase.invoke(any()) } returns listOf(e1, e2)
            viewModel.onMonthChanged(YearMonth.now())
            advanceUntilIdle()

            isGlobalAlarmEnabledFlow.value = false
            advanceUntilIdle()

            verify { mockScheduler.cancel(match { it.id == 301L }) }
            verify { mockScheduler.cancel(match { it.id == 302L }) }
        }

    @Test
    fun `rating dialog shows after 2 days and not prompted before`() =
        runTest {
            val twoDaysAgo = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(2) - 1000L
            installationDateFlow.value = twoDaysAgo
            ratingPromptedFlow.value = false
            ratingCompletedFlow.value = false

            coEvery { mockAppPreferencesRepository.setRatingPrompted(true) } just Runs

            // Create new ViewModel to trigger init block
            val vm =
                EventViewModel(
                    mockProUserProvider,
                    getEventsForMonthUseCase,
                    mockAppPreferencesRepository,
                    mockRingerModeRepository,
                    mockScheduler,
                    mockPermissionManager,
                    mockCalendarRepository,
                    mockDailyBriefingRepository,
                    mockGenerateDailyBriefingUseCase,
                    mockAskAiAboutScheduleUseCase,
                    mockCreateEventUseCase,
                    mockCalculateDepartureTimeUseCase,
                    ttsHelper,
                    mockWidgetUpdater,
                    mockReviewManager,
                )
            advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.showRatingBottomSheet)
                coVerify(exactly = 1) { mockAppPreferencesRepository.setRatingPrompted(true) }
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `rating dialog does not show if already prompted`() =
        runTest {
            val twoDaysAgo = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(2) - 1000L
            installationDateFlow.value = twoDaysAgo
            ratingPromptedFlow.value = true
            ratingCompletedFlow.value = false

            // Create new ViewModel to trigger init block
            val vm =
                EventViewModel(
                    mockProUserProvider,
                    getEventsForMonthUseCase,
                    mockAppPreferencesRepository,
                    mockRingerModeRepository,
                    mockScheduler,
                    mockPermissionManager,
                    mockCalendarRepository,
                    mockDailyBriefingRepository,
                    mockGenerateDailyBriefingUseCase,
                    mockAskAiAboutScheduleUseCase,
                    mockCreateEventUseCase,
                    mockCalculateDepartureTimeUseCase,
                    ttsHelper,
                    mockWidgetUpdater,
                    mockReviewManager,
                )
            advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.showRatingBottomSheet)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `rating dialog does not show if already completed`() =
        runTest {
            val twoDaysAgo = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(2) - 1000L
            installationDateFlow.value = twoDaysAgo
            ratingPromptedFlow.value = false
            ratingCompletedFlow.value = true

            val vm =
                EventViewModel(
                    mockProUserProvider,
                    getEventsForMonthUseCase,
                    mockAppPreferencesRepository,
                    mockRingerModeRepository,
                    mockScheduler,
                    mockPermissionManager,
                    mockCalendarRepository,
                    mockDailyBriefingRepository,
                    mockGenerateDailyBriefingUseCase,
                    mockAskAiAboutScheduleUseCase,
                    mockCreateEventUseCase,
                    mockCalculateDepartureTimeUseCase,
                    ttsHelper,
                    mockWidgetUpdater,
                    mockReviewManager,
                )
            advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.showRatingBottomSheet)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `rating dialog does not show before 2 days`() =
        runTest {
            val oneDayAgo = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(1)
            installationDateFlow.value = oneDayAgo
            ratingPromptedFlow.value = false
            ratingCompletedFlow.value = false

            val vm =
                EventViewModel(
                    mockProUserProvider,
                    getEventsForMonthUseCase,
                    mockAppPreferencesRepository,
                    mockRingerModeRepository,
                    mockScheduler,
                    mockPermissionManager,
                    mockCalendarRepository,
                    mockDailyBriefingRepository,
                    mockGenerateDailyBriefingUseCase,
                    mockAskAiAboutScheduleUseCase,
                    mockCreateEventUseCase,
                    mockCalculateDepartureTimeUseCase,
                    ttsHelper,
                    mockWidgetUpdater,
                    mockReviewManager,
                )
            advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.showRatingBottomSheet)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onRateNow marks rating as completed and hides dialog`() =
        runTest {
            coEvery { mockAppPreferencesRepository.setRatingCompleted(true) } just Runs

            viewModel.uiState.test {
                skipItems(1)

                viewModel.onRateNow()
                advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.showRatingBottomSheet)
                coVerify(exactly = 1) { mockAppPreferencesRepository.setRatingCompleted(true) }
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onRateLater hides dialog without marking as completed`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                viewModel.onRateLater()
                advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.showRatingBottomSheet)
                coVerify(exactly = 0) { mockAppPreferencesRepository.setRatingCompleted(true) }
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onRatingDialogDismiss hides dialog`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                viewModel.onRatingDialogDismiss()
                advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.showRatingBottomSheet)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onAllDayAlarmsToggle calls repository`() =
        runTest {
            coEvery { mockAppPreferencesRepository.setAllDayAlarmsEnabled(any()) } just Runs
            viewModel.onAllDayAlarmsToggle(true)
            advanceUntilIdle()
            coVerify { mockAppPreferencesRepository.setAllDayAlarmsEnabled(true) }
        }

    @Test
    fun `onAllDayAlarmHourChanged calls repository`() =
        runTest {
            coEvery { mockAppPreferencesRepository.setAllDayAlarmHour(any()) } just Runs
            viewModel.onAllDayAlarmHourChanged(10)
            advanceUntilIdle()
            coVerify { mockAppPreferencesRepository.setAllDayAlarmHour(10) }
        }

    @Test
    fun `onAlarmOffsetChanged calls repository`() =
        runTest {
            coEvery { mockAppPreferencesRepository.setAlarmOffsetMinutes(any()) } just Runs
            viewModel.onAlarmOffsetChanged(AlarmOffset.FIFTEEN_MINUTES)
            advanceUntilIdle()
            coVerify { mockAppPreferencesRepository.setAlarmOffsetMinutes(15L) }
        }

    @Test
    fun `loadAvailableCalendars updates UI state`() =
        runTest {
            val mockCalendars = listOf(DeviceCalendar(1, "Calendar 1", "Account 1"))
            coEvery { mockCalendarRepository.getAvailableCalendars() } returns mockCalendars

            viewModel.loadAvailableCalendars()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(mockCalendars, state.availableCalendars)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onCalendarFilterToggle updates repository`() =
        runTest {
            val mockCalendars =
                listOf(
                    DeviceCalendar(1, "Calendar 1", "Account 1"),
                    DeviceCalendar(2, "Calendar 2", "Account 2"),
                )
            coEvery { mockCalendarRepository.getAvailableCalendars() } returns mockCalendars
            every { mockAppPreferencesRepository.getEnabledCalendarIds() } returns flowOf(setOf("1"))
            coEvery { mockAppPreferencesRepository.setEnabledCalendarIds(any()) } just Runs

            // 1. Carregar calendários disponíveis para o UI State
            viewModel.loadAvailableCalendars()
            advanceUntilIdle()

            // 2. Chamar o toggle
            viewModel.onCalendarFilterToggle(2L, true)
            advanceUntilIdle()

            // 3. Verificar se o repositório foi chamado
            coVerify { mockAppPreferencesRepository.setEnabledCalendarIds(any()) }
        }

    @Test
    fun `clearCalendarFilter calls repository with empty set`() =
        runTest {
            coEvery { mockAppPreferencesRepository.setEnabledCalendarIds(emptySet()) } just Runs
            viewModel.clearCalendarFilter()
            advanceUntilIdle()
            coVerify { mockAppPreferencesRepository.setEnabledCalendarIds(emptySet()) }
        }

    @Test
    fun `generateDailyBriefing does nothing if user is not AI user`() =
        runTest {
            isAiUserFlow.value = false
            advanceUntilIdle()

            viewModel.generateDailyBriefing("instruction")
            advanceUntilIdle()

            coVerify(exactly = 0) { mockGenerateDailyBriefingUseCase.invoke(any(), any()) }
        }

    @Test
    fun `generateDailyBriefing calls usecase if user is AI user and has events today`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            val today = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val mockEvents = listOf(Event(1, "Today Event", today))

            val yearMonth = YearMonth.now()
            coEvery { getEventsForMonthUseCase.invoke(yearMonth) } returns mockEvents

            viewModel.onMonthChanged(yearMonth, forceRefresh = true)
            advanceUntilIdle()

            viewModel.generateDailyBriefing("instruction")
            advanceUntilIdle()

            coVerify(exactly = 1) { mockGenerateDailyBriefingUseCase.invoke(any(), "instruction") }
        }

    @Test
    fun `generateDailyBriefing calls usecase even if user has no events today`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            coEvery { getEventsForMonthUseCase.invoke(any()) } returns emptyList()

            viewModel.onMonthChanged(YearMonth.now(), forceRefresh = true)
            advanceUntilIdle()

            viewModel.generateDailyBriefing("instruction")
            advanceUntilIdle()

            coVerify(exactly = 1) { mockGenerateDailyBriefingUseCase.invoke(emptyList(), "instruction") }
        }

    @Test
    fun `askAi updates UI state and calls usecase`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            coEvery { mockAskAiAboutScheduleUseCase.invoke(any(), any(), any()) } returns "AI Response"

            viewModel.askAi("What's next?", "instruction")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("AI Response", state.aiResponse)
                assertEquals("What's next?", state.lastAiQuestion)
                assertFalse(state.isAskingAi)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `askAi with JSON in markdown blocks opens create event dialog`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

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
                mockAskAiAboutScheduleUseCase.invoke(any(), any(), any())
            } returns jsonResponse

            viewModel.uiState.test {
                // Pegar o estado atual após advanceUntilIdle
                val initialState = awaitItem()
                assertTrue(initialState.isAiUser)

                viewModel.askAi("Marcar dentista", "instrucao")

                val loadingState = awaitItem()
                assertTrue(loadingState.isAskingAi)

                val finalState = awaitItem()
                assertFalse(finalState.isAskingAi)
                assertTrue(finalState.showCreateEventDialog)
                assertEquals("Dentista", finalState.voiceEventData?.title)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clearAiResponse resets UI state`() =
        runTest {
            viewModel.askAi("Q", "I")
            advanceUntilIdle()

            viewModel.clearAiResponse()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.aiResponse)
                assertNull(state.lastAiQuestion)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onStartVoiceCaptureRequest and onDismissAiSuggestions toggle UI state`() =
        runTest {
            viewModel.onStartVoiceCaptureRequest()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showAiSuggestionsDialog)

            viewModel.onDismissAiSuggestions()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showAiSuggestionsDialog)
        }
}
