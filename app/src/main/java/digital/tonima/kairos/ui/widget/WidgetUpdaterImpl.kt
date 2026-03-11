package digital.tonima.kairos.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.utils.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = WidgetUpdater::class)
class WidgetUpdaterImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : WidgetUpdater {
        override suspend fun updateDailyBriefingWidget() {
            DailyBriefingWidget().updateAll(context)
        }
    }
