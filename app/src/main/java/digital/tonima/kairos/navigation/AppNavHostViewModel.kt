package digital.tonima.kairos.navigation

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavBackStack
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.kairos.core.navigation.FeatureNavKey
import javax.inject.Inject

/**
 * Thin bridge that exposes the [ActivityRetainedScoped][dagger.hilt.android.scopes.ActivityRetainedScoped]
 * [NavigationStateNavigator]'s backstack to Compose via `hiltViewModel()`, the same way every
 * feature screen already obtains its own ViewModel.
 */
@HiltViewModel
class AppNavHostViewModel
    @Inject
    constructor(
        navigator: NavigationStateNavigator,
    ) : ViewModel() {
        val backStack: NavBackStack<FeatureNavKey> = navigator.backStack
    }
