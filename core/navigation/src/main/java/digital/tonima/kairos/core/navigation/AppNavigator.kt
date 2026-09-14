package digital.tonima.kairos.core.navigation

/**
 * The one navigation seam a `:feature:*:impl` ViewModel is allowed to depend on. It lets a
 * feature request navigation to any [FeatureNavKey] — including one declared by another
 * feature's `:bridge` — without depending on that feature's `:impl`, `:app`'s composition
 * root, or any concrete backstack implementation.
 *
 * The real implementation ([digital.tonima.kairos.navigation.NavigationStateNavigator] as of
 * this writing) lives in `:app`, the only module that can see every feature's screens, and is
 * bound to this interface via a Hilt `@Binds`.
 */
interface AppNavigator {
    fun navigateTo(key: FeatureNavKey)

    fun popBackStack(): Boolean

    fun popTo(
        key: FeatureNavKey,
        inclusive: Boolean = false,
    )
}
