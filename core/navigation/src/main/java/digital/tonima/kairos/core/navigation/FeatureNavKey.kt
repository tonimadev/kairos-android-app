package digital.tonima.kairos.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Marker for every feature's own [NavKey] sealed hierarchy (e.g. `EventsNavKey`,
 * `AiNavKey`), declared in that feature's `:bridge` module. Purely structural: it lets
 * [AppNavigator] and the app-level backstack talk about "a destination in some feature"
 * without knowing which feature.
 */
interface FeatureNavKey : NavKey
