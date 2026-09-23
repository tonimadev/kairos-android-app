package digital.tonima.core.delegates

import digital.tonima.core.billing.BillingManager
import digital.tonima.core.billing.SubscriptionManager
import digital.tonima.core.repository.AppPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultProUserProviderTest {
    private val dispatcher = StandardTestDispatcher()
    private val inAppPro = MutableStateFlow(false)
    private val subscriptionPro = MutableStateFlow(false)
    private val billingManager: BillingManager = mockk(relaxed = true)
    private val subscriptionManager: SubscriptionManager = mockk(relaxed = true)
    private val preferences: AppPreferencesRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { billingManager.isProUser } returns inAppPro
        every { subscriptionManager.isProUser } returns subscriptionPro
        every { preferences.isProUser() } returns flowOf(false)
        every { preferences.isAiUser() } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `in-app purchase grants pro but not ai features`() =
        runTest(dispatcher) {
            val provider = DefaultProUserProvider(billingManager, subscriptionManager, preferences)

            inAppPro.value = true
            runCurrent()

            assertTrue(provider.isProUser.value)
            assertFalse(provider.isAiUser.value)
        }

    @Test
    fun `subscription grants both pro and ai features`() =
        runTest(dispatcher) {
            val provider = DefaultProUserProvider(billingManager, subscriptionManager, preferences)

            subscriptionPro.value = true
            runCurrent()

            assertTrue(provider.isProUser.value)
            assertTrue(provider.isAiUser.value)
        }

    @Test
    fun `no purchase grants nothing`() =
        runTest(dispatcher) {
            val provider = DefaultProUserProvider(billingManager, subscriptionManager, preferences)
            runCurrent()

            assertFalse(provider.isProUser.value)
            assertFalse(provider.isAiUser.value)
        }

    @Test
    fun `cancelling the subscription revokes ai but keeps pro from the one-time purchase`() =
        runTest(dispatcher) {
            val provider = DefaultProUserProvider(billingManager, subscriptionManager, preferences)
            inAppPro.value = true
            subscriptionPro.value = true
            runCurrent()

            subscriptionPro.value = false
            runCurrent()

            assertTrue(provider.isProUser.value)
            assertFalse(provider.isAiUser.value)
        }

    @Test
    fun `status changes are persisted`() =
        runTest(dispatcher) {
            DefaultProUserProvider(billingManager, subscriptionManager, preferences)

            subscriptionPro.value = true
            runCurrent()

            coVerify { preferences.setProUser(true) }
            coVerify { preferences.setAiUser(true) }
        }

    @Test
    fun `persisted status is exposed before the store answers`() {
        every { preferences.isProUser() } returns flowOf(true)
        every { preferences.isAiUser() } returns flowOf(true)

        // No dispatcher tick: this is the value screens read synchronously at startup.
        val provider = DefaultProUserProvider(billingManager, subscriptionManager, preferences)

        assertTrue(provider.isProUser.value)
        assertTrue(provider.isAiUser.value)
    }

    @Test
    fun `connects to the store on creation and forwards refresh`() {
        val provider = DefaultProUserProvider(billingManager, subscriptionManager, preferences)

        verify { billingManager.connect() }
        verify { subscriptionManager.connect() }

        provider.refresh()

        verify { billingManager.refresh() }
        verify { subscriptionManager.refresh() }
    }
}
