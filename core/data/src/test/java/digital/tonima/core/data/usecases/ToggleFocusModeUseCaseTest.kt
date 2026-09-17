package digital.tonima.core.data.usecases

import digital.tonima.core.data.repository.FocusModeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleFocusModeUseCaseTest {
    private lateinit var mockFocusModeRepository: FocusModeRepository
    private lateinit var useCase: ToggleFocusModeUseCaseImpl

    @Before
    fun setup() {
        mockFocusModeRepository = mockk()
        useCase = ToggleFocusModeUseCaseImpl(mockFocusModeRepository)
    }

    @Test
    fun `invoke sets do not disturb and succeeds when permission is granted`() {
        every { mockFocusModeRepository.hasNotificationPolicyAccess() } returns true
        every { mockFocusModeRepository.setDoNotDisturb(true) } returns Unit

        val result = useCase(true)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { mockFocusModeRepository.setDoNotDisturb(true) }
    }

    @Test
    fun `invoke fails without touching the repository when permission is not granted`() {
        every { mockFocusModeRepository.hasNotificationPolicyAccess() } returns false

        val result = useCase(true)

        assertTrue(result.isFailure)
        verify(exactly = 0) { mockFocusModeRepository.setDoNotDisturb(any()) }
    }

    @Test
    fun `hasAccess delegates to the repository`() {
        every { mockFocusModeRepository.hasNotificationPolicyAccess() } returns true

        assertTrue(useCase.hasAccess())
    }
}
