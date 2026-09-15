package digital.tonima.core.di

import javax.inject.Qualifier

/**
 * The AdMob banner unit id shown on [digital.tonima.kairos.ui.view.AlarmActivity]. The concrete
 * value depends on build-time config (env vars / local.properties / release signing) that only
 * `:app` has access to, so `:app` provides it via Hilt instead of `feature:alarm:impl` reading
 * `:app`'s BuildConfig directly, which would invert the module dependency direction.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AlarmBannerAdUnitId
