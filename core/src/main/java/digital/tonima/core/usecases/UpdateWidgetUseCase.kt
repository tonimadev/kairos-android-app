package digital.tonima.core.usecases

import digital.tonima.core.utils.WidgetUpdater
import javax.inject.Inject

class UpdateWidgetUseCase
    @Inject
    constructor(
        private val widgetUpdater: WidgetUpdater,
    ) {
        suspend fun updateDailyBriefingWidget() {
            widgetUpdater.updateDailyBriefingWidget()
        }
    }
