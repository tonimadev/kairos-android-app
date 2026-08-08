package digital.tonima.core.analytics

import digital.tonima.core.viewmodel.AiIntent.AskAi
import digital.tonima.core.viewmodel.AiIntent.GenerateDailyBriefing
import digital.tonima.core.viewmodel.AiIntent.SpeakAiResponse
import digital.tonima.core.viewmodel.BaseIntent
import digital.tonima.core.viewmodel.EventIntent.JoinMeeting
import digital.tonima.core.viewmodel.EventIntent.RateLater
import digital.tonima.core.viewmodel.EventIntent.RateNever
import digital.tonima.core.viewmodel.EventIntent.RateNow
import digital.tonima.core.viewmodel.EventIntent.ToggleCalendarFilter
import digital.tonima.core.viewmodel.EventIntent.ToggleEventAlarm
import digital.tonima.core.viewmodel.EventIntent.UpgradeToProIARequest
import digital.tonima.core.viewmodel.EventIntent.UpgradeToProRequest
import digital.tonima.core.viewmodel.SettingsIntent.ChangeTransportMode
import digital.tonima.core.viewmodel.SettingsIntent.ToggleAllDayAlarms
import digital.tonima.core.viewmodel.SettingsIntent.ToggleGlobalAlarms
import digital.tonima.core.viewmodel.SettingsIntent.ToggleLocationAlarm
import digital.tonima.core.viewmodel.SettingsIntent.ToggleSkipWeekends
import digital.tonima.core.viewmodel.SettingsIntent.ToggleVibrateOnly
import digital.tonima.core.viewmodel.SettingsIntent.UpdateAlarmOffset
import digital.tonima.core.viewmodel.SettingsIntent.UpdateAutoDismissMinutes
import digital.tonima.core.viewmodel.SettingsIntent.UpdateSnoozeTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventAnalytics
    @Inject
    constructor(
        private val analytics: Analytics,
    ) {
        fun logIntent(intent: BaseIntent) {
            when (intent) {
                is JoinMeeting -> analytics.logEvent(Analytics.EVENT_JOIN_MEETING)
                is ToggleGlobalAlarms ->
                    analytics.logEvent(
                        Analytics.EVENT_GLOBAL_ALARM_TOGGLE,
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is ToggleVibrateOnly ->
                    analytics.logEvent(
                        Analytics.EVENT_VIBRATE_TOGGLE,
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is ToggleAllDayAlarms ->
                    analytics.logEvent(
                        Analytics.EVENT_ALL_DAY_ALARM_TOGGLE,
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is ToggleEventAlarm ->
                    analytics.logEvent(
                        Analytics.EVENT_ALARM_TOGGLE,
                        mapOf(
                            Analytics.PARAM_ENABLED to intent.enabled,
                            Analytics.PARAM_ALL_OCCURRENCES to intent.allOccurrences,
                        ),
                    )
                is UpdateAlarmOffset ->
                    analytics.logEvent(
                        Analytics.EVENT_ALARM_OFFSET_CHANGED,
                        mapOf(Analytics.PARAM_VALUE to intent.offset.minutes),
                    )
                is UpdateSnoozeTime ->
                    analytics.logEvent(
                        Analytics.EVENT_SNOOZE_TIME_CHANGED,
                        mapOf(Analytics.PARAM_VALUE to intent.minutes),
                    )
                is ToggleSkipWeekends ->
                    analytics.logEvent(
                        "skip_weekends_toggle",
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is UpdateAutoDismissMinutes ->
                    analytics.logEvent(
                        "auto_dismiss_minutes_changed",
                        mapOf(Analytics.PARAM_VALUE to intent.minutes),
                    )
                is ToggleLocationAlarm ->
                    analytics.logEvent(
                        Analytics.EVENT_LOCATION_ALARM_TOGGLE,
                        mapOf(Analytics.PARAM_ENABLED to intent.enabled),
                    )
                is ChangeTransportMode ->
                    analytics.logEvent(
                        Analytics.EVENT_TRANSPORT_MODE_CHANGED,
                        mapOf(Analytics.PARAM_VALUE to intent.mode),
                    )
                is ToggleCalendarFilter ->
                    analytics.logEvent(
                        Analytics.EVENT_CALENDAR_FILTER_TOGGLE,
                        mapOf(
                            Analytics.PARAM_CALENDAR_ID to intent.calendarId,
                            Analytics.PARAM_ENABLED to intent.enabled,
                        ),
                    )
                is AskAi -> analytics.logEvent(Analytics.EVENT_AI_ASK)
                is GenerateDailyBriefing -> analytics.logEvent(Analytics.EVENT_AI_BRIEFING)
                SpeakAiResponse -> analytics.logEvent(Analytics.EVENT_AI_SPEAK)
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
