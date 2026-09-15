package digital.tonima.kairos.navigation

import androidx.navigation3.runtime.NavBackStack
import dagger.hilt.android.scopes.ActivityRetainedScoped
import digital.tonima.feature.calendar.bridge.CalendarNavKey
import digital.tonima.kairos.core.navigation.AppNavigator
import digital.tonima.kairos.core.navigation.FeatureNavKey
import javax.inject.Inject

/**
 * The real [AppNavigator] implementation. Lives in `:app` — the only module that can see every
 * feature's [FeatureNavKey] — and is bound to the `:core:navigation` contract via Hilt so any
 * feature ViewModel can request navigation without depending on this class or on `:app`.
 *
 * Scoped to [ActivityRetainedScoped] so the backstack survives configuration changes exactly
 * like the `hiltViewModel()`-obtained feature ViewModels already do, and is torn down with the
 * activity rather than living for the whole process.
 */
@ActivityRetainedScoped
class NavigationStateNavigator
    @Inject
    constructor() : AppNavigator {
        val backStack: NavBackStack<FeatureNavKey> = NavBackStack(CalendarNavKey.Main)

        override fun navigateTo(key: FeatureNavKey) {
            backStack.add(key)
        }

        override fun popBackStack(): Boolean {
            if (backStack.size <= 1) return false
            backStack.removeAt(backStack.lastIndex)
            return true
        }

        override fun popTo(
            key: FeatureNavKey,
            inclusive: Boolean,
        ) {
            val index = backStack.lastIndexOf(key)
            if (index < 0) return
            val truncateAt = if (inclusive) index else index + 1
            while (backStack.size > truncateAt) {
                backStack.removeAt(backStack.lastIndex)
            }
        }
    }
