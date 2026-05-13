package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ToggleEventVibrateUseCaseTest {
    private val repository = mockk<AppPreferencesRepository>(relaxed = true)
    private lateinit var useCase: ToggleEventVibrateUseCase

    private val sampleEvent =
        Event(
            id = 456L,
            title = "Vibrate Event",
            startTime = 2000000L,
            isAllDay = false,
        )

    @Before
    fun setup() {
        useCase = ToggleEventVibrateUseCase(repository)
    }

    @Test
    fun `when enabling vibrate only then event id is added to vibrate only list`() =
        runTest {
            coEvery { repository.getVibrateOnlyEventIds() } returns flowOf(setOf())

            useCase(sampleEvent, enabled = true)

            coVerify { repository.setVibrateOnlyEventIds(setOf(sampleEvent.uniqueIntentId.toString())) }
        }

    @Test
    fun `when disabling vibrate only then event id is removed from vibrate only list`() =
        runTest {
            coEvery { repository.getVibrateOnlyEventIds() } returns flowOf(setOf(sampleEvent.uniqueIntentId.toString()))

            useCase(sampleEvent, enabled = false)

            coVerify { repository.setVibrateOnlyEventIds(emptySet()) }
        }
}
