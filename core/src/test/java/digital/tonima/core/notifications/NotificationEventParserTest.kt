package digital.tonima.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class NotificationEventParserTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val now = LocalDateTime.of(2026, 10, 5, 10, 0).atZone(zone).toInstant().toEpochMilli()

    private fun parse(
        text: String,
        locale: Locale = Locale("pt", "BR"),
    ) = NotificationEventParser.parse(text, now, zone, locale)

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
    ) = LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `tomorrow with an hour in Portuguese becomes an event at that time`() {
        val event = parse("Vamos almoçar amanhã às 12h30?")

        assertNotNull(event)
        assertEquals(millis(2026, 10, 6, 12, 30), event!!.startMillis)
        assertEquals("Vamos almoçar", event.title)
    }

    @Test
    fun `default duration is one hour`() {
        val event = parse("Reunião amanhã 14:00")!!

        assertEquals(event.startMillis + 3_600_000L, event.endMillis)
    }

    @Test
    fun `numeric date without year uses the current year`() {
        val event = parse("Consulta dia 20/10 às 15h")

        assertEquals(millis(2026, 10, 20, 15), event!!.startMillis)
    }

    @Test
    fun `numeric date already past this year rolls over to next year`() {
        val event = parse("Reunião 01/03 14:00")

        assertEquals(millis(2027, 3, 1, 14), event!!.startMillis)
    }

    @Test
    fun `full date with four digit year is used as written`() {
        val event = parse("Meeting 15/12/2026 at 09:30")

        assertEquals(millis(2026, 12, 15, 9, 30), event!!.startMillis)
    }

    @Test
    fun `English relative date with pm is understood`() {
        val event = parse("Lunch meeting tomorrow at 3pm")

        assertEquals(millis(2026, 10, 6, 15), event!!.startMillis)
    }

    @Test
    fun `12 am is midnight and 12 pm is noon`() {
        assertEquals(millis(2026, 10, 6, 0), parse("meeting tomorrow 12am")!!.startMillis)
        assertEquals(millis(2026, 10, 6, 12), parse("meeting tomorrow 12pm")!!.startMillis)
    }

    @Test
    fun `today later than now is accepted`() {
        assertEquals(millis(2026, 10, 5, 18), parse("Call hoje às 18h")!!.startMillis)
    }

    @Test
    fun `today earlier than now is rejected`() {
        assertNull(parse("Call hoje às 08h"))
    }

    @Test
    fun `text without an appointment keyword is ignored`() {
        assertNull(parse("Chego amanhã às 15h"))
    }

    @Test
    fun `text without a date is ignored`() {
        assertNull(parse("Reunião às 15h"))
    }

    @Test
    fun `text without a time is ignored`() {
        assertNull(parse("Reunião amanhã"))
    }

    @Test
    fun `impossible calendar dates are ignored`() {
        assertNull(parse("Reunião 31/02/2027 14:00"))
    }

    @Test
    fun `dates more than a year away are ignored`() {
        assertNull(parse("Meeting 10/10/2030 14:00"))
    }

    @Test
    fun `a decimal number is not mistaken for a date`() {
        assertNull(parse("Reunião 3.5 às 15h"))
    }

    @Test
    fun `long titles are truncated`() {
        val event = parse("Reunião " + "muito ".repeat(30) + "longa amanhã 14:00")!!

        assertEquals(true, event.title.length <= 60)
    }
}
