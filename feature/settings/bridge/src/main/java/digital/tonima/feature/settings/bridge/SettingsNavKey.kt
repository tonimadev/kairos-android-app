package digital.tonima.feature.settings.bridge

import digital.tonima.kairos.core.navigation.AppNavigator
import digital.tonima.kairos.core.navigation.FeatureNavKey

/** Destinations owned by the settings feature, reachable through [AppNavigator]. */
sealed interface SettingsNavKey : FeatureNavKey {
    data object Root : SettingsNavKey
}
