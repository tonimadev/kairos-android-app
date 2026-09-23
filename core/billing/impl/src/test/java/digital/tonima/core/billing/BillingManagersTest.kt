package digital.tonima.core.billing

import android.app.Activity
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagersTest {
    private val dispatcher = StandardTestDispatcher()
    private val payWall = FakePayWallManager()
    private val activity: Activity = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Ownership

    @Test
    fun `remove ads purchase unlocks the in-app pro status only`() =
        runTest(dispatcher) {
            val billing = BillingManagerImpl(payWall)
            val subscription = SubscriptionManagerImpl(payWall)

            payWall.ownedProductIds.value = setOf(PRODUCT_ID_REMOVE_ADS)
            runCurrent()

            assertTrue(billing.isProUser.value)
            assertFalse("A one-time purchase must not unlock the AI subscription", subscription.isProUser.value)
        }

    @Test
    fun `monthly subscription unlocks the subscription status only`() =
        runTest(dispatcher) {
            val billing = BillingManagerImpl(payWall)
            val subscription = SubscriptionManagerImpl(payWall)

            payWall.ownedProductIds.value = setOf(MONTHLY_SUBSCRIPTION_PLAN)
            runCurrent()

            assertTrue(subscription.isProUser.value)
            assertFalse(billing.isProUser.value)
        }

    @Test
    fun `unrelated products unlock nothing`() =
        runTest(dispatcher) {
            val billing = BillingManagerImpl(payWall)
            val subscription = SubscriptionManagerImpl(payWall)

            payWall.ownedProductIds.value = setOf("some_other_product")
            runCurrent()

            assertFalse(billing.isProUser.value)
            assertFalse(subscription.isProUser.value)
        }

    @Test
    fun `losing ownership revokes the status`() =
        runTest(dispatcher) {
            val subscription = SubscriptionManagerImpl(payWall)
            payWall.ownedProductIds.value = setOf(MONTHLY_SUBSCRIPTION_PLAN)
            runCurrent()

            payWall.ownedProductIds.value = emptySet()
            runCurrent()

            assertFalse("Cancelled or refunded subscriptions must be revoked", subscription.isProUser.value)
        }

    // endregion

    // region Purchase flow

    @Test
    fun `purchase launches immediately when the store is ready`() =
        runTest(dispatcher) {
            payWall.isReady.value = true

            BillingManagerImpl(payWall).launchPurchaseFlow(activity)

            assertEquals(listOf(PRODUCT_ID_REMOVE_ADS), payWall.purchases)
            assertEquals(0, payWall.connectCalls)
        }

    @Test
    fun `purchase tapped before the store is ready launches once it connects`() =
        runTest(dispatcher) {
            val billing = BillingManagerImpl(payWall)

            billing.launchPurchaseFlow(activity)
            runCurrent()
            assertEquals(1, payWall.connectCalls)
            assertTrue(payWall.purchases.isEmpty())

            advanceTimeBy(5_000)
            payWall.isReady.value = true
            runCurrent()

            assertEquals(listOf(PRODUCT_ID_REMOVE_ADS), payWall.purchases)
        }

    @Test
    fun `purchase reports an error when the store never becomes ready`() =
        runTest(dispatcher) {
            val billing = BillingManagerImpl(payWall)
            var errors = 0
            backgroundScope.launch { billing.purchaseErrors.collect { errors++ } }
            runCurrent()

            billing.launchPurchaseFlow(activity)
            advanceTimeBy(15_001)
            runCurrent()

            assertEquals(1, errors)
            assertTrue(payWall.purchases.isEmpty())
        }

    // endregion

    // region Subscription flow

    @Test
    fun `subscription launches the monthly plan when the store is ready`() =
        runTest(dispatcher) {
            payWall.isReady.value = true

            SubscriptionManagerImpl(payWall).launchSubscriptionFlow(activity)

            assertEquals(listOf(MONTHLY_SUBSCRIPTION_PLAN to MONTHLY_SUBSCRIPTION_PLAN), payWall.subscriptions)
        }

    @Test
    fun `subscription tapped before the store is ready launches once it connects`() =
        runTest(dispatcher) {
            val subscription = SubscriptionManagerImpl(payWall)

            subscription.launchSubscriptionFlow(activity)
            runCurrent()
            payWall.isReady.value = true
            runCurrent()

            assertEquals(1, payWall.connectCalls)
            assertEquals(1, payWall.subscriptions.size)
        }

    @Test
    fun `subscription reports an error after waiting 15 seconds for the store`() =
        runTest(dispatcher) {
            val subscription = SubscriptionManagerImpl(payWall)
            val error = backgroundScope.launch { subscription.subscriptionErrors.first() }
            runCurrent()

            subscription.launchSubscriptionFlow(activity)
            advanceTimeBy(14_999)
            runCurrent()
            assertFalse("Must keep waiting before the timeout", error.isCompleted)

            advanceTimeBy(2)
            runCurrent()
            assertTrue(error.isCompleted)
            assertTrue(payWall.subscriptions.isEmpty())
        }

    // endregion

    @Test
    fun `connect and refresh are forwarded to the store`() =
        runTest(dispatcher) {
            val billing = BillingManagerImpl(payWall)
            val subscription = SubscriptionManagerImpl(payWall)

            billing.connect()
            subscription.connect()
            billing.refresh()
            subscription.refresh()

            assertEquals(2, payWall.connectCalls)
            assertEquals(2, payWall.refreshCalls)
        }
}
