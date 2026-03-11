package digital.tonima.kairos.wear.service

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.utils.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = WidgetUpdater::class)
class WearWidgetUpdaterImpl
    @Inject
    constructor() : WidgetUpdater {
        override suspend fun updateDailyBriefingWidget() {
            // No-op no Wear OS (Glance widgets são específicos do Mobile)
        }
    }
