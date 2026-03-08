package digital.tonima.core.delegates

import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.SINGLETON
import digital.tonima.core.billing.BillingManager
import digital.tonima.core.billing.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@BindType(installIn = SINGLETON, to = ProUserProvider::class)
class DefaultProUserProvider
    @Inject
    constructor(
        billingManager: BillingManager,
        subscriptionManager: SubscriptionManager,
    ) : ProUserProvider {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        override val isProUser: StateFlow<Boolean> =
            combine(
                billingManager.isProUser,
                subscriptionManager.isProUser,
            ) { inApp, sub -> inApp || sub }
                .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

        override val isAiUser: StateFlow<Boolean> = subscriptionManager.isProUser
    }
