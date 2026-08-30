package digital.tonima.core.viewmodel

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import digital.tonima.core.usecases.GetGoogleSignInIntentUseCase
import digital.tonima.core.usecases.HandleGoogleSignInResultUseCase
import digital.tonima.core.usecases.IsGoogleSignedInUseCase
import digital.tonima.core.usecases.SignOutFromGoogleUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class AuthViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val mockIsGoogleSignedIn: IsGoogleSignedInUseCase = mockk(relaxed = true)
    private val mockGetGoogleSignInIntent: GetGoogleSignInIntentUseCase = mockk(relaxed = true)
    private val mockHandleGoogleSignInResultUseCase: HandleGoogleSignInResultUseCase = mockk(relaxed = true)
    private val mockSignOutFromGoogle: SignOutFromGoogleUseCase = mockk(relaxed = true)

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { mockIsGoogleSignedIn() } returns false

        viewModel =
            AuthViewModel(
                mockIsGoogleSignedIn,
                mockGetGoogleSignInIntent,
                mockHandleGoogleSignInResultUseCase,
                mockSignOutFromGoogle,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signInWithGoogle emits LaunchGoogleSignIn side effect`() =
        runTest {
            val mockIntent = mockk<Intent>()
            every { mockGetGoogleSignInIntent() } returns mockIntent

            viewModel.handleIntent(AuthIntent.SignInWithGoogle)
            advanceUntilIdle()

            val effect = viewModel.uiState.value.effect
            assertTrue(effect is AuthSideEffect.LaunchGoogleSignIn)
            assertEquals(
                mockIntent,
                (effect as AuthSideEffect.LaunchGoogleSignIn).intent,
            )
        }

    @Test
    fun `signOutFromGoogle updates UI state and calls use case`() =
        runTest {
            coEvery { mockSignOutFromGoogle() } just Runs

            // Assume initially signed in for this test
            every { mockIsGoogleSignedIn() } returns true
            val vm =
                AuthViewModel(
                    mockIsGoogleSignedIn,
                    mockGetGoogleSignInIntent,
                    mockHandleGoogleSignInResultUseCase,
                    mockSignOutFromGoogle,
                )

            vm.handleIntent(AuthIntent.SignOutFromGoogle)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isGoogleConnected)
            coVerify { mockSignOutFromGoogle() }
        }

    @Test
    fun `handleGoogleSignInResult success updates UI state`() =
        runTest {
            val mockResultData = mockk<Intent>()
            coEvery { mockHandleGoogleSignInResultUseCase(mockResultData) } returns Result.success(Unit)

            viewModel.handleIntent(AuthIntent.HandleGoogleSignInResult(mockResultData))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isGoogleConnected)
        }
}
