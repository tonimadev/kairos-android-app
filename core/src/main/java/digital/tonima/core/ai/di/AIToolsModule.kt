package digital.tonima.core.ai.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.tools.CategorizeEventTool
import digital.tonima.core.ai.tools.CreateEventTool
import digital.tonima.core.ai.tools.ManageFocusModeTool
import digital.tonima.core.ai.tools.NotifyLateTool
import digital.tonima.core.ai.tools.RescheduleEventTool
import digital.tonima.core.ai.tools.SearchTool
import digital.tonima.core.ai.tools.SuggestFocusBlocksTool
import digital.tonima.core.ai.tools.ToggleGlobalAlarmsTool
import javax.inject.Singleton

/**
 * Hilt module that provides all [AITool] implementations into a `Set<AITool>`
 * multibinding, which is then injected into [digital.tonima.core.ai.ActionRegistry].
 *
 * To register a new tool simply add another `@Provides @IntoSet` function here.
 */
@Module
@InstallIn(SingletonComponent::class)
object AIToolsModule {
    @Provides
    @IntoSet
    @Singleton
    fun provideCreateEventTool(): AITool = CreateEventTool()

    @Provides
    @IntoSet
    @Singleton
    fun provideToggleGlobalAlarmsTool(): AITool = ToggleGlobalAlarmsTool()

    @Provides
    @IntoSet
    @Singleton
    fun provideSearchTool(): AITool = SearchTool()

    @Provides
    @IntoSet
    @Singleton
    fun provideNotifyLateTool(): AITool = NotifyLateTool()

    @Provides
    @IntoSet
    @Singleton
    fun provideManageFocusModeTool(): AITool = ManageFocusModeTool()

    @Provides
    @IntoSet
    @Singleton
    fun provideRescheduleEventTool(): AITool = RescheduleEventTool()

    @Provides
    @IntoSet
    @Singleton
    fun provideCategorizeEventTool(): AITool = CategorizeEventTool()

    @Provides
    @IntoSet
    @Singleton
    fun provideSuggestFocusBlocksTool(): AITool = SuggestFocusBlocksTool()
}
