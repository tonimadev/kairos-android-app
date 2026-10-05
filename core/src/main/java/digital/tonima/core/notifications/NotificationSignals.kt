package digital.tonima.core.notifications

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** An alarm that just rang. Held in memory only, for a short time; never persisted. */
data class FiredAlarm(
    val title: String,
    val startTime: Long,
    val firedAt: Long,
)

/**
 * In-process bridge between the alarm pipeline and the (optional) notification listener.
 *
 * The alarm side only publishes facts here; it never depends on the listener, so alarms behave
 * exactly the same when notification access is not granted.
 */
object NotificationSignals {
    private const val RECENT_ALARM_TTL_MS = 3 * 60 * 60 * 1000L
    private const val MAX_RECENT_ALARMS = 32
    private const val BUFFER = 16
    private const val MAX_FOCUS_DURATION_MS = 8 * 60 * 60 * 1000L

    private val lock = Any()
    private val recent = ArrayDeque<FiredAlarm>()
    private val firedFlow =
        MutableSharedFlow<FiredAlarm>(extraBufferCapacity = BUFFER, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val focusEndedFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @Volatile
    private var focusWindow: LongRange = LongRange.EMPTY

    val alarmFired: SharedFlow<FiredAlarm> = firedFlow.asSharedFlow()
    val focusEnded: SharedFlow<Unit> = focusEndedFlow.asSharedFlow()

    fun alarmFired(
        title: String,
        startTime: Long,
        now: Long = System.currentTimeMillis(),
    ) {
        val alarm = FiredAlarm(title, startTime, now)
        synchronized(lock) {
            prune(now)
            recent.addLast(alarm)
            while (recent.size > MAX_RECENT_ALARMS) recent.removeFirst()
        }
        firedFlow.tryEmit(alarm)
    }

    fun recentAlarms(now: Long = System.currentTimeMillis()): List<FiredAlarm> =
        synchronized(lock) {
            prune(now)
            recent.toList()
        }

    /**
     * Remembers the meeting that is about to start. Open-ended or very long events (all-day events,
     * for instance) are ignored: holding notifications for the whole day would be wrong.
     */
    fun focusScheduled(
        startTime: Long,
        endTime: Long,
    ) {
        val duration = endTime - startTime
        if (startTime <= 0L || duration <= 0L || duration > MAX_FOCUS_DURATION_MS) return
        focusWindow = startTime until endTime
    }

    fun focusEnded(now: Long = System.currentTimeMillis()) {
        // A back-to-back meeting may already be scheduled; only forget a window that is over.
        if (now > focusWindow.last) focusWindow = LongRange.EMPTY
        focusEndedFlow.tryEmit(Unit)
    }

    /** End of the meeting in progress at [now], or 0 when none is. */
    fun activeFocusEnd(now: Long = System.currentTimeMillis()): Long {
        val window = focusWindow
        return if (now in window) window.last + 1 else 0L
    }

    internal fun resetForTest() {
        synchronized(lock) { recent.clear() }
        focusWindow = LongRange.EMPTY
    }

    private fun prune(now: Long) {
        while (recent.isNotEmpty() && now - recent.first().firedAt > RECENT_ALARM_TTL_MS) recent.removeFirst()
    }
}
