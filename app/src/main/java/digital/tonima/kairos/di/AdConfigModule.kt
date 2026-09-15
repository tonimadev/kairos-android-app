package digital.tonima.kairos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import digital.tonima.core.di.AlarmBannerAdUnitId
import digital.tonima.kairos.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object AdConfigModule {
    @Provides
    @AlarmBannerAdUnitId
    fun provideAlarmBannerAdUnitId(): String = BuildConfig.ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY
}
