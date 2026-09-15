package digital.tonima.kairos.navigation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import digital.tonima.kairos.core.navigation.AppNavigator

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class NavigationModule {
    @Binds
    abstract fun bindAppNavigator(impl: NavigationStateNavigator): AppNavigator
}
