package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.EventAlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ToggleEventAlarmUseCaseTest {
    private val repository = mockk<AppPreferencesRepository>(relaxed = true)
    private val scheduler = mockk<EventAlarmScheduler>(relaxed = true)
    private lateinit var useCase: ToggleEventAlarmUseCase

    private val sampleEvent =
        Event(
            id = 123L,
            title = "Sample Event",
            startTime = 1000000L,
            isAllDay = false,
        )

    @Before
    fun setup() {
        useCase = ToggleEventAlarmUseCase(repository, scheduler)
        coEvery { repository.isGlobalAlarmEnabled() } returns flowOf(true)
    }

    @Test
    fun `when disabling single event occurrence then event id is added to disabled list`() =
        runTest {
            coEvery { repository.getDisabledEventIds() } returns flowOf(setOf())
            coEvery { repository.getDisabledSeriesIds() } returns flowOf(setOf())

            useCase(sampleEvent, isEnabled = false, disableAllOccurrences = false)

            coVerify { repository.setDisabledEventIds(setOf(sampleEvent.uniqueIntentId.toString())) }
            coVerify { scheduler.cancel(sampleEvent) }
        }

    @Test
    fun `when enabling single event occurrence then event id is removed from disabled list`() =
        runTest {
            coEvery { repository.getDisabledEventIds() } returns flowOf(setOf(sampleEvent.uniqueIntentId.toString()))
            coEvery { repository.getDisabledSeriesIds() } returns flowOf(setOf())

            useCase(sampleEvent, isEnabled = true, disableAllOccurrences = false)

            coVerify { repository.setDisabledEventIds(emptySet()) }
            coVerify { scheduler.schedule(sampleEvent) }
        }

    @Test
    fun `when disabling all occurrences then series id is added to disabled list`() =
        runTest {
            coEvery { repository.getDisabledEventIds() } returns flowOf(setOf())
            coEvery { repository.getDisabledSeriesIds() } returns flowOf(setOf())

            useCase(sampleEvent, isEnabled = false, disableAllOccurrences = true)

            coVerify { repository.setDisabledSeriesIds(setOf(sampleEvent.id.toString())) }
            coVerify { scheduler.cancel(sampleEvent) }
        }

    @Test
    fun `when enabling all occurrences then series id is removed from disabled list`() =
        runTest {
            coEvery { repository.getDisabledEventIds() } returns flowOf(setOf())
            coEvery { repository.getDisabledSeriesIds() } returns flowOf(setOf(sampleEvent.id.toString()))

            useCase(sampleEvent, isEnabled = true, disableAllOccurrences = true)

            coVerify { repository.setDisabledSeriesIds(emptySet()) }
            coVerify { scheduler.schedule(sampleEvent) }
        }

    @Test
    fun `when global alarm is disabled then scheduler is not called`() =
        runTest {
            coEvery { repository.isGlobalAlarmEnabled() } returns flowOf(false)
            coEvery { repository.getDisabledEventIds() } returns flowOf(setOf())
            coEvery { repository.getDisabledSeriesIds() } returns flowOf(setOf())

            useCase(sampleEvent, isEnabled = true, disableAllOccurrences = false)

            coVerify(exactly = 0) { scheduler.schedule(any()) }
            coVerify(exactly = 0) { scheduler.cancel(any()) }
        }
}
