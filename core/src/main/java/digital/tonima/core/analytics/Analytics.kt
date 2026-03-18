package digital.tonima.core.analytics

interface Analytics {
    fun logEvent(
        name: String,
        params: Map<String, Any?> = emptyMap(),
    )

    companion object {
        const val EVENT_ALARM_SNOOZE = "alarm_snooze"
        const val EVENT_ALARM_STOP = "alarm_stop"
        const val EVENT_ALARM_FIRED = "alarm_fired"
        const val EVENT_JOIN_MEETING = "join_meeting"
        const val EVENT_ALARM_TOGGLE = "alarm_toggle"
        const val EVENT_GLOBAL_ALARM_TOGGLE = "global_alarm_toggle"
        const val EVENT_VIBRATE_TOGGLE = "vibrate_toggle"
        const val EVENT_ALL_DAY_ALARM_TOGGLE = "all_day_alarm_toggle"
        const val EVENT_LOCATION_ALARM_TOGGLE = "location_alarm_toggle"
        const val EVENT_CALENDAR_FILTER_TOGGLE = "calendar_filter_toggle"
        const val EVENT_EVENT_CREATED = "event_created"
        const val EVENT_AI_ASK = "ai_ask"
        const val EVENT_AI_BRIEFING = "ai_briefing_generated"
        const val EVENT_AI_SPEAK = "ai_speak"
        const val EVENT_UPGRADE_REQUEST = "upgrade_request"
        const val EVENT_RATE_NOW = "rate_now"
        const val EVENT_RATE_LATER = "rate_later"
        const val EVENT_RATE_NEVER = "rate_never"
        const val EVENT_ALARM_OFFSET_CHANGED = "alarm_offset_changed"
        const val EVENT_SNOOZE_TIME_CHANGED = "snooze_time_changed"
        const val EVENT_TRANSPORT_MODE_CHANGED = "transport_mode_changed"

        const val PARAM_SOURCE = "source"
        const val PARAM_ENABLED = "enabled"
        const val PARAM_EVENT_TITLE = "event_title"
        const val PARAM_CALENDAR_ID = "calendar_id"
        const val PARAM_ALL_OCCURRENCES = "all_occurrences"
        const val PARAM_VALUE = "value"
        const val PARAM_MEETING_URL_PRESENT = "meeting_url_present"

        const val SOURCE_ACTIVITY = "activity"
        const val SOURCE_NOTIFICATION = "notification"
    }
}
