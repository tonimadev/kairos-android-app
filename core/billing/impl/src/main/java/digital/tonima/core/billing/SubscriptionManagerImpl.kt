package digital.tonima.core.billing

import android.app.Activity
import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.SINGLETON
import digital.tonima.paywall.core.PayWallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin adapter over the shared [PayWallManager], keeping the
 * [SubscriptionManager] contract the rest of the app already depends on. All
 * the native BillingClient handling (connection, queries, acknowledgment,
 * offer token resolution) now lives in the PayWall SDK instead of being
 * duplicated here.
 */
@Singleton
@BindType(installIn = SINGLETON, to = SubscriptionManager::class)
class SubscriptionManagerImpl
    @Inject
    constructor(
        private val payWallManager: PayWallManager,
    ) : SubscriptionManager {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _subscriptionErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val subscriptionErrors: Flow<Unit> = _subscriptionErrors.asSharedFlow()

        override val isProUser: StateFlow<Boolean> =
            payWallManager.ownedProductIds
                .map { owned -> owned.contains(MONTHLY_SUBSCRIPTION_PLAN) }
                .stateIn(scope, SharingStarted.Eagerly, false)

        override fun connect() {
            payWallManager.connect()
        }

        override fun launchSubscriptionFlow(activity: Activity) {
            if (payWallManager.isReady.value) {
                payWallManager.launchSubscription(activity, MONTHLY_SUBSCRIPTION_PLAN, MONTHLY_SUBSCRIPTION_PLAN)
                return
            }
            // Not ready yet (e.g. connection still in progress or was retried
            // in the background) - connect and retry once it comes up, instead
            // of silently dropping the tap.
            payWallManager.connect()
            scope.launch {
                val becameReady =
                    withTimeoutOrNull(READY_TIMEOUT_MS) {
                        payWallManager.isReady.first { it }
                        true
                    } ?: false
                if (becameReady) {
                    payWallManager.launchSubscription(activity, MONTHLY_SUBSCRIPTION_PLAN, MONTHLY_SUBSCRIPTION_PLAN)
                } else {
                    _subscriptionErrors.tryEmit(Unit)
                }
            }
        }

        override fun refresh() {
            payWallManager.refresh()
        }

        private companion object {
            const val READY_TIMEOUT_MS = 15_000L
        }
    }
