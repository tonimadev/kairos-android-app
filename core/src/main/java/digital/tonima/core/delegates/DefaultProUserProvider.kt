package digital.tonima.core.delegates

import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.SINGLETON
import digital.tonima.core.billing.BillingManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@BindType(installIn = SINGLETON, to = ProUserProvider::class)
class DefaultProUserProvider
    @Inject
    constructor(
        billingManager: BillingManager,
    ) : ProUserProvider {
        override val isProUser: StateFlow<Boolean> = billingManager.isProUser
    }
