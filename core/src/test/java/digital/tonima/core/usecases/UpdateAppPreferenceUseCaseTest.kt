package digital.tonima.core.usecases

import digital.tonima.core.repository.AppPreferencesRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UpdateAppPreferenceUseCaseTest {
    private val repository = mockk<AppPreferencesRepository>(relaxed = true)
    private lateinit var useCase: UpdateAppPreferenceUseCase

    @Before
    fun setup() {
        useCase = UpdateAppPreferenceUseCase(repository)
    }

    @Test
    fun `when setGlobalAlarmEnabled is called then repository should be updated`() =
        runTest {
            useCase.setGlobalAlarmEnabled(true)
            coVerify { repository.setGlobalAlarmEnabled(true) }
        }

    @Test
    fun `when setVibrateOnly is called then repository should be updated`() =
        runTest {
            useCase.setVibrateOnly(true)
            coVerify { repository.setVibrateOnly(true) }
        }

    @Test
    fun `when setAllDayAlarmsEnabled is called then repository should be updated`() =
        runTest {
            useCase.setAllDayAlarmsEnabled(true)
            coVerify { repository.setAllDayAlarmsEnabled(true) }
        }

    @Test
    fun `when setAllDayAlarmHour is called then repository should be updated`() =
        runTest {
            useCase.setAllDayAlarmHour(10)
            coVerify { repository.setAllDayAlarmHour(10) }
        }

    @Test
    fun `when setAlarmOffsetMinutes is called then repository should be updated`() =
        runTest {
            useCase.setAlarmOffsetMinutes(15L)
            coVerify { repository.setAlarmOffsetMinutes(15L) }
        }

    @Test
    fun `when setSnoozeTimeMinutes is called then repository should be updated`() =
        runTest {
            useCase.setSnoozeTimeMinutes(5)
            coVerify { repository.setSnoozeTimeMinutes(5) }
        }

    @Test
    fun `when setSkipWeekendsEnabled is called then repository should be updated`() =
        runTest {
            useCase.setSkipWeekendsEnabled(true)
            coVerify { repository.setSkipWeekendsEnabled(true) }
        }

    @Test
    fun `when setAutoDismissMinutes is called then repository should be updated`() =
        runTest {
            useCase.setAutoDismissMinutes(3)
            coVerify { repository.setAutoDismissMinutes(3) }
        }

    @Test
    fun `when setAutostartSuggestionDismissed is called then repository should be updated`() =
        runTest {
            useCase.setAutostartSuggestionDismissed(true)
            coVerify { repository.setAutostartSuggestionDismissed(true) }
        }

    @Test
    fun `when setLocationAlarmEnabled is called then repository should be updated`() =
        runTest {
            useCase.setLocationAlarmEnabled(true)
            coVerify { repository.setLocationAlarmEnabled(true) }
        }

    @Test
    fun `when setPreferredTransportMode is called then repository should be updated`() =
        runTest {
            useCase.setPreferredTransportMode("walking")
            coVerify { repository.setPreferredTransportMode("walking") }
        }

    @Test
    fun `when setEnabledCalendarIds is called then repository should be updated`() =
        runTest {
            val ids = setOf("1", "2")
            useCase.setEnabledCalendarIds(ids)
            coVerify { repository.setEnabledCalendarIds(ids) }
        }

    @Test
    fun `when setDisabledEventIds is called then repository should be updated`() =
        runTest {
            val ids = setOf("101", "102")
            useCase.setDisabledEventIds(ids)
            coVerify { repository.setDisabledEventIds(ids) }
        }

    @Test
    fun `when setDisabledSeriesIds is called then repository should be updated`() =
        runTest {
            val ids = setOf("series1")
            useCase.setDisabledSeriesIds(ids)
            coVerify { repository.setDisabledSeriesIds(ids) }
        }

    @Test
    fun `when setVibrateOnlyEventIds is called then repository should be updated`() =
        runTest {
            val ids = setOf("event1")
            useCase.setVibrateOnlyEventIds(ids)
            coVerify { repository.setVibrateOnlyEventIds(ids) }
        }

    @Test
    fun `when setRatingPrompted is called then repository should be updated`() =
        runTest {
            useCase.setRatingPrompted(true)
            coVerify { repository.setRatingPrompted(true) }
        }

    @Test
    fun `when setRatingCompleted is called then repository should be updated`() =
        runTest {
            useCase.setRatingCompleted(true)
            coVerify { repository.setRatingCompleted(true) }
        }

    @Test
    fun `when setExactAlarmPermissionSkipped is called then repository should be updated`() =
        runTest {
            useCase.setExactAlarmPermissionSkipped(true)
            coVerify { repository.setExactAlarmPermissionSkipped(true) }
        }

    @Test
    fun `when setFullScreenIntentPermissionSkipped is called then repository should be updated`() =
        runTest {
            useCase.setFullScreenIntentPermissionSkipped(true)
            coVerify { repository.setFullScreenIntentPermissionSkipped(true) }
        }

    @Test
    fun `when setTemperatureInCelsius is called then repository should be updated`() =
        runTest {
            useCase.setTemperatureInCelsius(true)
            coVerify { repository.setTemperatureInCelsius(true) }
        }
}
