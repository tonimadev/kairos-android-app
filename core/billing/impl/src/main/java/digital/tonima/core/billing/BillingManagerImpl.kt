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
import kotlin.time.Duration.Companion.milliseconds

/**
 * Thin adapter over the shared [PayWallManager], keeping the [BillingManager]
 * contract the rest of the app already depends on. All the native
 * BillingClient handling (connection, queries, acknowledgment) now lives in
 * the PayWall SDK instead of being duplicated here.
 */
@Singleton
@BindType(installIn = SINGLETON, to = BillingManager::class)
class BillingManagerImpl
    @Inject
    constructor(
        private val payWallManager: PayWallManager,
    ) : BillingManager {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _purchaseErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val purchaseErrors: Flow<Unit> = _purchaseErrors.asSharedFlow()

        override val isProUser: StateFlow<Boolean> =
            payWallManager.ownedProductIds
                .map { owned -> owned.contains(PRODUCT_ID_REMOVE_ADS) }
                .stateIn(scope, SharingStarted.Eagerly, false)

        override fun connect() {
            payWallManager.connect()
        }

        override fun launchPurchaseFlow(activity: Activity) {
            if (payWallManager.isReady.value) {
                payWallManager.launchPurchase(activity, PRODUCT_ID_REMOVE_ADS)
                return
            }
            // Not ready yet (e.g. connection still in progress or was retried
            // in the background) - connect and retry once it comes up, instead
            // of silently dropping the tap.
            payWallManager.connect()
            scope.launch {
                val becameReady =
                    withTimeoutOrNull(READY_TIMEOUT_MS.milliseconds) {
                        payWallManager.isReady.first { it }
                        true
                    } ?: false
                if (becameReady) {
                    payWallManager.launchPurchase(activity, PRODUCT_ID_REMOVE_ADS)
                } else {
                    _purchaseErrors.tryEmit(Unit)
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
