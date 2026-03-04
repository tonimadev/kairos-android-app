package digital.tonima.core.model

/**
 * How many minutes before the event the alarm should fire.
 */
enum class AlarmOffset(val minutes: Long) {
    AT_TIME(0),
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    ONE_HOUR(60);

    companion object {
        fun fromMinutes(minutes: Long): AlarmOffset =
            entries.firstOrNull { it.minutes == minutes } ?: AT_TIME
    }
}

