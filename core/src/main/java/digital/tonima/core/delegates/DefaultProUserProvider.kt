package digital.tonima.core.delegates

import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.SINGLETON
import digital.tonima.core.billing.BillingManager
import digital.tonima.core.billing.SubscriptionManager
import digital.tonima.core.repository.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@BindType(installIn = SINGLETON, to = ProUserProvider::class)
class DefaultProUserProvider
    @Inject
    constructor(
        private val billingManager: BillingManager,
        private val subscriptionManager: SubscriptionManager,
        private val appPreferencesRepository: AppPreferencesRepository,
    ) : ProUserProvider {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        private val initialProStatus =
            runBlocking {
                appPreferencesRepository.isProUser().first()
            }

        private val initialAiStatus =
            runBlocking {
                appPreferencesRepository.isAiUser().first()
            }

        override val isProUser: StateFlow<Boolean> =
            combine(
                billingManager.isProUser,
                subscriptionManager.isProUser,
            ) { inApp, sub -> inApp || sub }
                .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialProStatus)

        override val isAiUser: StateFlow<Boolean> =
            subscriptionManager.isProUser
                .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialAiStatus)

        init {
            billingManager.connect()
            subscriptionManager.connect()

            // Persistir status pro
            isProUser.onEach { isPro ->
                appPreferencesRepository.setProUser(isPro)
            }.launchIn(scope)

            // Persistir status AI
            isAiUser.onEach { isAi ->
                appPreferencesRepository.setAiUser(isAi)
            }.launchIn(scope)
        }

        override fun refresh() {
            billingManager.refresh()
            subscriptionManager.refresh()
        }
    }
