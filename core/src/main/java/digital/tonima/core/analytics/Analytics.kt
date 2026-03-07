package digital.tonima.core.analytics

interface Analytics {
    fun logEvent(
        name: String,
        params: Map<String, Any?> = emptyMap(),
    )

    companion object {
        const val EVENT_ALARM_SNOOZE = "alarm_snooze"
        const val EVENT_ALARM_STOP = "alarm_stop"
        const val PARAM_SOURCE = "source"
        const val SOURCE_ACTIVITY = "activity"
        const val SOURCE_NOTIFICATION = "notification"
    }
}
