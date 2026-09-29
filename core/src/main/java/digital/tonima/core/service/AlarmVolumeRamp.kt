package digital.tonima.core.service

/** Volume the ringtone starts at when escalating volume or the spoken announcement is enabled. */
const val ALARM_START_VOLUME = 0.15f
const val ALARM_FULL_VOLUME = 1f
const val ALARM_VOLUME_RAMP_MS = 30_000L
const val ALARM_VOLUME_STEP_MS = 1_000L

/** Ringtone volume [elapsedMs] after the ramp started: linear from [ALARM_START_VOLUME] to full volume. */
fun alarmVolumeAt(
    elapsedMs: Long,
    rampMs: Long = ALARM_VOLUME_RAMP_MS,
): Float {
    if (rampMs <= 0L || elapsedMs >= rampMs) return ALARM_FULL_VOLUME
    val progress = elapsedMs.coerceAtLeast(0L).toFloat() / rampMs
    return ALARM_START_VOLUME + (ALARM_FULL_VOLUME - ALARM_START_VOLUME) * progress
}

/** Longest the ringtone stays quiet for the spoken announcement before going back to full volume. */
const val ALARM_ANNOUNCE_TIMEOUT_MS = 30_000L

/** Volume the ringtone starts at: quiet when it will ramp up or be talked over, full otherwise. */
fun alarmInitialVolume(
    escalatingVolume: Boolean,
    announceEvent: Boolean,
): Float = if (escalatingVolume || announceEvent) ALARM_START_VOLUME else ALARM_FULL_VOLUME
