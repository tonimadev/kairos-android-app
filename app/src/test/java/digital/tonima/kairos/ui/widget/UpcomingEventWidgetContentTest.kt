package digital.tonima.kairos.ui.widget

import digital.tonima.kairos.core.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class UpcomingEventWidgetContentTest {
    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val now = at(2026, 9, 29, 20, 0)

    @Test
    fun `when there is no next event then the widget says so`() {
        assertEquals(UpcomingEventContent.NoEvents, upcomingEventContent(null, now, zone))
    }

    @Test
    fun `when the event starts within 24 hours then a live countdown to its start is shown`() {
        val event = timedEvent(start = at(2026, 9, 29, 21, 30))

        val content = upcomingEventContent(event, now, zone) as UpcomingEventContent.Next

        assertEquals(EventDay.TODAY, content.day)
        assertEquals(Countdown.StartsAt(event.startTime), content.countdown)
    }

    @Test
    fun `when the event is tomorrow but within 24 hours then it is labelled tomorrow with a live countdown`() {
        val event = timedEvent(start = at(2026, 9, 30, 9, 0))

        val content = upcomingEventContent(event, now, zone) as UpcomingEventContent.Next

        assertEquals(EventDay.TOMORROW, content.day)
        assertEquals(Countdown.StartsAt(event.startTime), content.countdown)
    }

    @Test
    fun `when the event is more than 24 hours away then the countdown is in days`() {
        val event = timedEvent(start = at(2026, 10, 2, 9, 0))

        val content = upcomingEventContent(event, now, zone) as UpcomingEventContent.Next

        assertEquals(EventDay.LATER, content.day)
        assertEquals(Countdown.InDays(3), content.countdown)
    }

    @Test
    fun `when the event is tomorrow but more than 24 hours away then it counts one day`() {
        val event = timedEvent(start = at(2026, 9, 30, 21, 0))

        val content = upcomingEventContent(event, now, zone) as UpcomingEventContent.Next

        assertEquals(EventDay.TOMORROW, content.day)
        assertEquals(Countdown.InDays(1), content.countdown)
    }

    @Test
    fun `when the event has started and not ended then it is happening now`() {
        val event = timedEvent(start = at(2026, 9, 29, 19, 30), end = at(2026, 9, 29, 20, 30))

        val content = upcomingEventContent(event, now, zone) as UpcomingEventContent.Next

        assertEquals(EventDay.TODAY, content.day)
        assertEquals(Countdown.Now, content.countdown)
    }

    @Test
    fun `an all-day event today is happening now even late in the local evening`() {
        // Stored at UTC midnight: read in the local zone it would fall on the previous day.
        val event = allDayEvent(LocalDate.of(2026, 9, 29))

        val content = upcomingEventContent(event, at(2026, 9, 29, 20, 59), zone) as UpcomingEventContent.Next

        assertEquals(EventDay.TODAY, content.day)
        assertEquals(Countdown.Now, content.countdown)
    }

    @Test
    fun `an all-day event on a later day counts the days until it`() {
        val event = allDayEvent(LocalDate.of(2026, 10, 1))

        val content = upcomingEventContent(event, now, zone) as UpcomingEventContent.Next

        assertEquals(EventDay.LATER, content.day)
        assertEquals(Countdown.InDays(2), content.countdown)
    }

    @Test
    fun `an all-day event tomorrow is labelled tomorrow`() {
        val content =
            upcomingEventContent(allDayEvent(LocalDate.of(2026, 9, 30)), now, zone) as UpcomingEventContent.Next

        assertEquals(EventDay.TOMORROW, content.day)
        assertEquals(Countdown.InDays(1), content.countdown)
    }

    @Test
    fun `refresh when the live countdown should start for an event more than 24 hours away`() {
        val event = timedEvent(start = at(2026, 10, 2, 9, 0))

        assertEquals(at(2026, 9, 30, 0, 0), nextWidgetRefreshAt(event, now, zone))
        assertEquals(
            event.startTime - TimeUnit.HOURS.toMillis(24),
            nextWidgetRefreshAt(event, at(2026, 10, 1, 8, 0), zone),
        )
    }

    @Test
    fun `refresh when the event starts and again when it ends`() {
        val event = timedEvent(start = at(2026, 9, 29, 21, 0), end = at(2026, 9, 29, 22, 0))

        assertEquals(event.startTime, nextWidgetRefreshAt(event, now, zone))
        assertEquals(event.endTime, nextWidgetRefreshAt(event, at(2026, 9, 29, 21, 10), zone))
    }

    @Test
    fun `refresh at midnight so that tomorrow becomes today`() {
        val event = timedEvent(start = at(2026, 9, 30, 9, 0), end = at(2026, 9, 30, 10, 0))

        assertEquals(at(2026, 9, 30, 0, 0), nextWidgetRefreshAt(event, now, zone))
    }

    @Test
    fun `refresh an all-day event at local midnight or when it ends, whichever comes first`() {
        val event = allDayEvent(LocalDate.of(2026, 9, 29))

        // West of UTC the event ends (UTC midnight) before local midnight.
        assertEquals(event.endTime, nextWidgetRefreshAt(event, at(2026, 9, 29, 8, 0), zone))

        // East of UTC local midnight comes first, and the day count must change then.
        val tokyo = ZoneId.of("Asia/Tokyo")
        val tokyoEvening = LocalDateTime.of(2026, 9, 29, 20, 0).atZone(tokyo).toInstant().toEpochMilli()
        val tokyoMidnight = LocalDate.of(2026, 9, 30).atStartOfDay(tokyo).toInstant().toEpochMilli()
        assertEquals(tokyoMidnight, nextWidgetRefreshAt(event, tokyoEvening, tokyo))
    }

    @Test
    fun `no refresh is scheduled when there is no event`() {
        assertNull(nextWidgetRefreshAt(null, now, zone))
    }

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun timedEvent(
        start: Long,
        end: Long = start + TimeUnit.HOURS.toMillis(1),
    ) = Event(id = 1L, title = "Reunião", startTime = start, endTime = end)

    private fun allDayEvent(date: LocalDate): Event {
        val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return Event(
            id = 2L,
            title = "Feriado",
            startTime = start,
            endTime = start + TimeUnit.DAYS.toMillis(1),
            isAllDay = true,
        )
    }
}
