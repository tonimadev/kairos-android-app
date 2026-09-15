package digital.tonima.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class IcsParserTest {
    @Test
    fun `parseIcs should parse events correctly`() {
        val icsContent =
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Kairos//App//EN
            BEGIN:VEVENT
            UID:event1@kairos
            DTSTART:20260603T103000Z
            DTEND:20260603T113000Z
            SUMMARY:Reunião Importante
            LOCATION:Sala 1
            END:VEVENT
            BEGIN:VEVENT
            UID:event2@kairos
            DTSTART;VALUE=DATE:20260604
            SUMMARY:Dia Todo
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()

        val events = IcsParser.parseIcs(icsContent)

        assertEquals(2, events.size)

        // Check Event 1
        val event1 = events[0]
        assertEquals("Reunião Importante", event1.title)
        assertEquals("Sala 1", event1.location)
        assertEquals(false, event1.isAllDay)

        val format =
            SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        val expectedStart = format.parse("20260603T103000Z")?.time
        val expectedEnd = format.parse("20260603T113000Z")?.time
        assertEquals(expectedStart, event1.startTime)
        assertEquals(expectedEnd, event1.endTime)

        // Check Event 2
        val event2 = events[1]
        assertEquals("Dia Todo", event2.title)
        assertEquals(true, event2.isAllDay)

        val formatAllDay =
            SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        val expectedAllDayStart = formatAllDay.parse("20260604")?.time
        assertEquals(expectedAllDayStart, event2.startTime)
    }
}
