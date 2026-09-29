package digital.tonima.core.data.usecases

import digital.tonima.core.repository.AppPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleGlobalAlarmsUseCaseTest {
    private val repository: AppPreferencesRepository = mockk(relaxUnitFun = true)
    private val useCase = ToggleGlobalAlarmsUseCaseImpl(repository)

    @Test
    fun `when alarms are enabled then invoke disables them and returns false`() =
        runTest {
            every { repository.isGlobalAlarmEnabled() } returns flowOf(true)

            val result = useCase()

            assertFalse(result)
            coVerify(exactly = 1) { repository.setGlobalAlarmEnabled(false) }
        }

    @Test
    fun `when alarms are disabled then invoke enables them and returns true`() =
        runTest {
            every { repository.isGlobalAlarmEnabled() } returns flowOf(false)

            val result = useCase()

            assertTrue(result)
            coVerify(exactly = 1) { repository.setGlobalAlarmEnabled(true) }
        }

    @Test(expected = java.io.IOException::class)
    fun `when the preference cannot be written then the failure propagates to the caller`() =
        runTest {
            every { repository.isGlobalAlarmEnabled() } returns flowOf(true)
            coEvery { repository.setGlobalAlarmEnabled(any()) } throws java.io.IOException("disk full")

            useCase()
        }
}
