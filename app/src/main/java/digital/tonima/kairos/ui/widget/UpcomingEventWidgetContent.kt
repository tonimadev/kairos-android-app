package digital.tonima.kairos.ui.widget

import digital.tonima.kairos.core.model.Event
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** What the upcoming event widget shows. */
sealed interface UpcomingEventContent {
    data object NoCalendarPermission : UpcomingEventContent

    data object NoEvents : UpcomingEventContent

    data object Unavailable : UpcomingEventContent

    data class Next(
        val event: Event,
        val day: EventDay,
        val countdown: Countdown,
    ) : UpcomingEventContent
}

enum class EventDay { TODAY, TOMORROW, LATER }

sealed interface Countdown {
    /** The event has started and has not ended yet. */
    data object Now : Countdown

    /** Starts within [COUNTDOWN_WINDOW_MS]: shown as a live timer counting down to [startTime]. */
    data class StartsAt(val startTime: Long) : Countdown

    /** Starts on a later calendar day, [days] from today. */
    data class InDays(val days: Int) : Countdown
}

/** Timed events closer than this get a live countdown instead of a day count. */
val COUNTDOWN_WINDOW_MS: Long = TimeUnit.HOURS.toMillis(24)

fun upcomingEventContent(
    event: Event?,
    now: Long,
    zone: ZoneId,
): UpcomingEventContent {
    if (event == null) return UpcomingEventContent.NoEvents
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val eventDate = eventDate(event, zone)
    val daysAway = ChronoUnit.DAYS.between(today, eventDate).toInt()
    val day =
        when (daysAway) {
            // An event that started yesterday and is still running is shown as today's.
            in Int.MIN_VALUE..0 -> EventDay.TODAY
            1 -> EventDay.TOMORROW
            else -> EventDay.LATER
        }
    val countdown =
        when {
            event.isAllDay -> if (daysAway <= 0) Countdown.Now else Countdown.InDays(daysAway)
            event.startTime <= now -> Countdown.Now
            event.startTime - now < COUNTDOWN_WINDOW_MS -> Countdown.StartsAt(event.startTime)
            else -> Countdown.InDays(daysAway.coerceAtLeast(1))
        }
    return UpcomingEventContent.Next(event, day, countdown)
}

/**
 * When the widget must be redrawn so it does not go stale: when the countdown should start ticking,
 * when the event starts, when it ends (the next event takes its place) and, for day labels, at midnight.
 */
fun nextWidgetRefreshAt(
    event: Event?,
    now: Long,
    zone: ZoneId,
): Long? {
    if (event == null) return null
    val nextMidnight =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
    val boundaries =
        if (event.isAllDay) {
            listOf(nextMidnight, event.endTime)
        } else {
            listOf(event.startTime - COUNTDOWN_WINDOW_MS, event.startTime, event.endTime, nextMidnight)
        }
    return boundaries.filter { it > now }.minOrNull()
}

/** All-day events are stored at UTC midnight, so their day must be read in UTC. */
fun eventDate(
    event: Event,
    zone: ZoneId,
): LocalDate {
    val eventZone = if (event.isAllDay) ZoneOffset.UTC else zone
    return Instant.ofEpochMilli(event.startTime).atZone(eventZone).toLocalDate()
}
