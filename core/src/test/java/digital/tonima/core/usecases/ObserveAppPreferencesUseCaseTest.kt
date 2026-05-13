package digital.tonima.core.usecases

import app.cash.turbine.test
import digital.tonima.core.repository.AppPreferencesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveAppPreferencesUseCaseTest {
    private val repository = mockk<AppPreferencesRepository>()
    private lateinit var useCase: ObserveAppPreferencesUseCase

    @Before
    fun setup() {
        useCase = ObserveAppPreferencesUseCase(repository)
    }

    @Test
    fun `when repository emits values then usecase combines them into AppPreferences`() =
        runTest {
            coEvery { repository.isGlobalAlarmEnabled() } returns flowOf(true)
            coEvery { repository.getVibrateOnly() } returns flowOf(false)
            coEvery { repository.isAllDayAlarmsEnabled() } returns flowOf(true)
            coEvery { repository.getAllDayAlarmHour() } returns flowOf(9)
            coEvery { repository.getAlarmOffsetMinutes() } returns flowOf(15L)
            coEvery { repository.isLocationAlarmEnabled() } returns flowOf(false)
            coEvery { repository.getPreferredTransportMode() } returns flowOf("driving")
            coEvery { repository.getEnabledCalendarIds() } returns flowOf(setOf("1"))
            coEvery { repository.getSnoozeTimeMinutes() } returns flowOf(10)
            coEvery { repository.getAutostartSuggestionDismissed() } returns flowOf(false)
            coEvery { repository.getDisabledEventIds() } returns flowOf(setOf("e1"))
            coEvery { repository.getDisabledSeriesIds() } returns flowOf(setOf("s1"))
            coEvery { repository.getVibrateOnlyEventIds() } returns flowOf(setOf("v1"))
            coEvery { repository.isExactAlarmPermissionSkipped() } returns flowOf(false)
            coEvery { repository.isFullScreenIntentPermissionSkipped() } returns flowOf(false)
            coEvery { repository.isSkipWeekendsEnabled() } returns flowOf(true)
            coEvery { repository.getAutoDismissMinutes() } returns flowOf(5)
            coEvery { repository.isTemperatureInCelsius() } returns flowOf(true)

            useCase().test {
                val prefs = awaitItem()
                assertEquals(true, prefs.isGlobalAlarmEnabled)
                assertEquals(false, prefs.vibrateOnly)
                assertEquals(true, prefs.allDayAlarmsEnabled)
                assertEquals(9, prefs.allDayAlarmHour)
                assertEquals(15L, prefs.alarmOffsetMinutes)
                assertEquals(false, prefs.isLocationAlarmEnabled)
                assertEquals("driving", prefs.preferredTransportMode)
                assertEquals(setOf("1"), prefs.enabledCalendarIds)
                assertEquals(10, prefs.snoozeTimeMinutes)
                assertEquals(false, prefs.autostartSuggestionDismissed)
                assertEquals(setOf("e1"), prefs.disabledEventIds)
                assertEquals(setOf("s1"), prefs.disabledSeriesIds)
                assertEquals(setOf("v1"), prefs.vibrateOnlyEventIds)
                assertEquals(false, prefs.exactAlarmPermissionSkipped)
                assertEquals(false, prefs.fullScreenIntentPermissionSkipped)
                assertEquals(true, prefs.skipWeekendsEnabled)
                assertEquals(5, prefs.autoDismissMinutes)
                assertEquals(true, prefs.isTemperatureInCelsius)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
