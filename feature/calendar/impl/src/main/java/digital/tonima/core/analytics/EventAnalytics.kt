package digital.tonima.core.analytics

import digital.tonima.core.viewmodel.EventIntent
import digital.tonima.core.viewmodel.EventIntent.JoinMeeting
import digital.tonima.core.viewmodel.EventIntent.RateLater
import digital.tonima.core.viewmodel.EventIntent.RateNever
import digital.tonima.core.viewmodel.EventIntent.RateNow
import digital.tonima.core.viewmodel.EventIntent.ToggleCalendarFilter
import digital.tonima.core.viewmodel.EventIntent.ToggleEventAlarm
import digital.tonima.core.viewmodel.EventIntent.UpgradeToProIARequest
import digital.tonima.core.viewmodel.EventIntent.UpgradeToProRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventAnalytics
    @Inject
    constructor(
        private val analytics: Analytics,
    ) {
        fun logIntent(intent: EventIntent) {
            when (intent) {
                is JoinMeeting -> analytics.logEvent(Analytics.EVENT_JOIN_MEETING)
                is ToggleEventAlarm ->
                    analytics.logEvent(
                        Analytics.EVENT_ALARM_TOGGLE,
                        mapOf(
                            Analytics.PARAM_ENABLED to intent.enabled,
                            Analytics.PARAM_ALL_OCCURRENCES to intent.allOccurrences,
                        ),
                    )
                is ToggleCalendarFilter ->
                    analytics.logEvent(
                        Analytics.EVENT_CALENDAR_FILTER_TOGGLE,
                        mapOf(
                            Analytics.PARAM_CALENDAR_ID to intent.calendarId,
                            Analytics.PARAM_ENABLED to intent.enabled,
                        ),
                    )
                UpgradeToProRequest -> analytics.logEvent(Analytics.EVENT_UPGRADE_REQUEST)
                UpgradeToProIARequest -> analytics.logEvent(Analytics.EVENT_UPGRADE_IA_REQUEST)
                is RateNow -> analytics.logEvent(Analytics.EVENT_RATE_NOW)
                RateLater -> analytics.logEvent(Analytics.EVENT_RATE_LATER)
                RateNever -> analytics.logEvent(Analytics.EVENT_RATE_NEVER)
                else -> Unit
            }
        }

        fun logEventCreated() {
            analytics.logEvent(Analytics.EVENT_EVENT_CREATED)
        }
    }
