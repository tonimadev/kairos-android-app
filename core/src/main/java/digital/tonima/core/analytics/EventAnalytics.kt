package digital.tonima.core.analytics

import digital.tonima.core.viewmodel.EventIntent
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
                is EventIntent.JoinMeeting -> analytics.logEvent(Analytics.EVENT_JOIN_MEETING)
                is EventIntent.ToggleGlobalAlarms ->
                    analytics.logEvent(
                        Analytics.EVENT_GLOBAL_ALARM_TOGGLE,
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is EventIntent.ToggleVibrateOnly ->
                    analytics.logEvent(
                        Analytics.EVENT_VIBRATE_TOGGLE,
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is EventIntent.ToggleAllDayAlarms ->
                    analytics.logEvent(
                        Analytics.EVENT_ALL_DAY_ALARM_TOGGLE,
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is EventIntent.ToggleEventAlarm ->
                    analytics.logEvent(
                        Analytics.EVENT_ALARM_TOGGLE,
                        mapOf(
                            Analytics.PARAM_ENABLED to intent.enabled,
                            Analytics.PARAM_ALL_OCCURRENCES to intent.allOccurrences,
                        ),
                    )
                is EventIntent.UpdateAlarmOffset ->
                    analytics.logEvent(
                        Analytics.EVENT_ALARM_OFFSET_CHANGED,
                        mapOf(Analytics.PARAM_VALUE to intent.offset.minutes),
                    )
                is EventIntent.UpdateSnoozeTime ->
                    analytics.logEvent(
                        Analytics.EVENT_SNOOZE_TIME_CHANGED,
                        mapOf(Analytics.PARAM_VALUE to intent.minutes),
                    )
                is EventIntent.ToggleSkipWeekends ->
                    analytics.logEvent(
                        "skip_weekends_toggle",
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is EventIntent.UpdateAutoDismissMinutes ->
                    analytics.logEvent(
                        "auto_dismiss_minutes_changed",
                        mapOf(Analytics.PARAM_VALUE to intent.minutes),
                    )
                is EventIntent.ToggleLocationAlarm ->
                    analytics.logEvent(
                        Analytics.EVENT_LOCATION_ALARM_TOGGLE,
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is EventIntent.ChangeTransportMode ->
                    analytics.logEvent(
                        Analytics.EVENT_TRANSPORT_MODE_CHANGED,
                        mapOf(Analytics.PARAM_VALUE to intent.mode),
                    )
                is EventIntent.ToggleCalendarFilter ->
                    analytics.logEvent(
                        Analytics.EVENT_CALENDAR_FILTER_TOGGLE,
                        mapOf(
                            Analytics.PARAM_CALENDAR_ID to intent.calendarId,
                            Analytics.PARAM_ENABLED to intent.enabled,
                        ),
                    )
                is EventIntent.AskAi -> analytics.logEvent(Analytics.EVENT_AI_ASK)
                is EventIntent.GenerateDailyBriefing -> analytics.logEvent(Analytics.EVENT_AI_BRIEFING)
                EventIntent.SpeakAiResponse -> analytics.logEvent(Analytics.EVENT_AI_SPEAK)
                EventIntent.UpgradeToProRequest -> analytics.logEvent(Analytics.EVENT_UPGRADE_REQUEST)
                EventIntent.UpgradeToProIARequest -> analytics.logEvent(Analytics.EVENT_UPGRADE_IA_REQUEST)
                is EventIntent.RateNow -> analytics.logEvent(Analytics.EVENT_RATE_NOW)
                EventIntent.RateLater -> analytics.logEvent(Analytics.EVENT_RATE_LATER)
                EventIntent.RateNever -> analytics.logEvent(Analytics.EVENT_RATE_NEVER)
                else -> Unit
            }
        }

        fun logEventCreated() {
            analytics.logEvent(Analytics.EVENT_EVENT_CREATED)
        }
    }
