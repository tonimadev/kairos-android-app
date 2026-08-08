package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.usecases.AppPreferences
import digital.tonima.core.usecases.CheckPermissionsUseCase
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.ObserveRingerModeUseCase
import digital.tonima.core.usecases.PermissionState
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class SettingsViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val mockObserveAppPreferencesUseCase: ObserveAppPreferencesUseCase = mockk(relaxed = true)
    private val mockUpdateAppPreferenceUseCase: UpdateAppPreferenceUseCase = mockk(relaxed = true)
    private val mockCheckPermissionsUseCase: CheckPermissionsUseCase = mockk(relaxed = true)
    private val mockObserveRingerModeUseCase: ObserveRingerModeUseCase = mockk(relaxed = true)

    private val appPreferencesFlow = MutableStateFlow(defaultAppPreferences())
    private val ringerModeFlow = MutableStateFlow(AudioWarningState.NORMAL)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { mockObserveAppPreferencesUseCase() } returns appPreferencesFlow
        every { mockObserveRingerModeUseCase() } returns ringerModeFlow
        every { mockCheckPermissionsUseCase() } returns
            PermissionState(
                hasCalendarPermission = true,
                hasPostNotificationsPermission = true,
                hasExactAlarmPermission = true,
                hasFullScreenIntentPermission = true,
                hasLocationPermission = false,
                hasBackgroundLocationPermission = false,
            )

        viewModel =
            SettingsViewModel(
                mockObserveAppPreferencesUseCase,
                mockUpdateAppPreferenceUseCase,
                mockCheckPermissionsUseCase,
                mockObserveRingerModeUseCase,
            )
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

    @Test
    fun `checkPermissions updates all permission flags in UI state`() =
        runTest {
            every { mockCheckPermissionsUseCase() } returns
                PermissionState(
                    hasCalendarPermission = false,
                    hasPostNotificationsPermission = false,
                    hasExactAlarmPermission = true,
                    hasFullScreenIntentPermission = true,
                    hasLocationPermission = false,
                    hasBackgroundLocationPermission = false,
                )

            viewModel.handleIntent(SettingsIntent.CheckPermissions)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.hasCalendarPermission)
            assertFalse(state.hasPostNotificationsPermission)
            assertTrue(state.hasExactAlarmPermission)
            assertTrue(state.hasFullScreenIntentPermission)
        }

    @Test
    fun `dismissAutostartSuggestion sets dismissed flag in preferences`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setAutostartSuggestionDismissed(true) } just Runs

            viewModel.handleIntent(SettingsIntent.DismissAutostartSuggestion)
            advanceUntilIdle()

            coVerify { mockUpdateAppPreferenceUseCase.setAutostartSuggestionDismissed(true) }
        }

    @Test
    fun `onAlarmsToggle sets global alarm enabled status in preferences`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setGlobalAlarmEnabled(any()) } just Runs

            viewModel.handleIntent(SettingsIntent.ToggleGlobalAlarms(false))
            advanceUntilIdle()
            coVerify { mockUpdateAppPreferenceUseCase.setGlobalAlarmEnabled(false) }
        }

    @Test
    fun `onVibrateOnlyChanged persists preference`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setVibrateOnly(any()) } just Runs

            viewModel.handleIntent(SettingsIntent.ToggleVibrateOnly(true))
            advanceUntilIdle()
            coVerify { mockUpdateAppPreferenceUseCase.setVibrateOnly(true) }
        }

    @Test
    fun `onAlarmOffsetChanged calls use case`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setAlarmOffsetMinutes(any()) } just Runs
            viewModel.handleIntent(SettingsIntent.UpdateAlarmOffset(AlarmOffset.FIFTEEN_MINUTES))
            advanceUntilIdle()
            coVerify { mockUpdateAppPreferenceUseCase.setAlarmOffsetMinutes(15L) }
        }

    @Test
    fun `SkipExactAlarmPermission sets skipped flag and updates UI state`() =
        runTest {
            coEvery { mockUpdateAppPreferenceUseCase.setExactAlarmPermissionSkipped(true) } just Runs

            viewModel.handleIntent(SettingsIntent.SkipExactAlarmPermission)
            advanceUntilIdle()

            coVerify { mockUpdateAppPreferenceUseCase.setExactAlarmPermissionSkipped(true) }
            assertTrue(viewModel.uiState.value.hasExactAlarmPermission)
        }
}
