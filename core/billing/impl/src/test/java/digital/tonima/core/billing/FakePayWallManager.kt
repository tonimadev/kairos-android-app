package digital.tonima.core.billing

import android.app.Activity
import digital.tonima.paywall.core.PayWallManager
import kotlinx.coroutines.flow.MutableStateFlow

class FakePayWallManager : PayWallManager {
    override val ownedProductIds = MutableStateFlow<Set<String>>(emptySet())
    override val isReady = MutableStateFlow(false)

    var connectCalls = 0
    var refreshCalls = 0
    val purchases = mutableListOf<String>()
    val subscriptions = mutableListOf<Pair<String, String?>>()

    override fun connect() {
        connectCalls++
    }

    override fun disconnect() = Unit

    override fun launchPurchase(
        activity: Activity,
        productId: String,
    ) {
        purchases += productId
    }

    override fun launchSubscription(
        activity: Activity,
        productId: String,
        basePlanId: String?,
    ) {
        subscriptions += productId to basePlanId
    }

    override fun refresh() {
        refreshCalls++
    }
}
