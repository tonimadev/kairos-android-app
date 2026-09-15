package digital.tonima.kairos.wear.navigation

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.kairos.core.navigation.AppNavigator
import digital.tonima.kairos.core.navigation.FeatureNavKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wear has its own screen model (see [digital.tonima.kairos.wear.ui.WearApp]) instead of a
 * Navigation 3 backstack — it never fires the Open/Close screen intents that call into
 * [AppNavigator] on phone. This satisfies [EventViewModel][digital.tonima.core.viewmodel.EventViewModel] and
 * [SettingsViewModel][digital.tonima.core.viewmodel.SettingsViewModel]'s constructor dependency
 * on the wear Hilt graph without pulling in `:app`'s NavDisplay-backed implementation.
 */
@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = AppNavigator::class)
class NoOpAppNavigator
    @Inject
    constructor() : AppNavigator {
        override fun navigateTo(key: FeatureNavKey) = Unit

        override fun popBackStack(): Boolean = false

        override fun popTo(
            key: FeatureNavKey,
            inclusive: Boolean,
        ) = Unit
    }
