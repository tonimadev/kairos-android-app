package digital.tonima.core.usecases

import digital.tonima.core.analytics.EventAnalytics
import digital.tonima.core.viewmodel.EventIntent
import javax.inject.Inject

class LogEventUseCase
    @Inject
    constructor(
        private val eventAnalytics: EventAnalytics,
    ) {
        operator fun invoke(intent: EventIntent) {
            eventAnalytics.logIntent(intent)
        }

        fun logEventCreated() {
            eventAnalytics.logEventCreated()
        }
    }
