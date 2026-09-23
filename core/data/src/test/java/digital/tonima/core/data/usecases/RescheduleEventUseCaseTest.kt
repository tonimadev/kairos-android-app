package digital.tonima.core.data.usecases

import digital.tonima.core.data.repository.CalendarRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RescheduleEventUseCaseTest {
    private val repository: CalendarRepository = mockk()
    private val useCase = RescheduleEventUseCase(repository)

    @Test
    fun `a single event is moved to the new time`() =
        runTest {
            coEvery { repository.isRecurring(42L) } returns false
            coEvery { repository.rescheduleEvent(42L, 5_000L, 6_000L) } returns true

            assertEquals(RescheduleResult.Success, useCase(42L, 5_000L, 6_000L))
        }

    @Test
    fun `recurring events are refused so the whole series is not moved`() =
        runTest {
            coEvery { repository.isRecurring(42L) } returns true

            assertEquals(RescheduleResult.RecurringNotSupported, useCase(42L, 5_000L, 6_000L))
            coVerify(exactly = 0) { repository.rescheduleEvent(any(), any(), any()) }
        }

    @Test
    fun `an end before the start is refused without touching the calendar`() =
        runTest {
            assertEquals(RescheduleResult.InvalidTime, useCase(42L, 6_000L, 5_000L))
            assertEquals(RescheduleResult.InvalidTime, useCase(42L, 5_000L, 5_000L))
            coVerify(exactly = 0) { repository.rescheduleEvent(any(), any(), any()) }
        }

    @Test
    fun `a failed update is reported`() =
        runTest {
            coEvery { repository.isRecurring(42L) } returns false
            coEvery { repository.rescheduleEvent(any(), any(), any()) } returns false

            assertEquals(RescheduleResult.Failed, useCase(42L, 5_000L, 6_000L))
        }
}
