package digital.tonima.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `parseIcs should honor the TZID timezone on DTSTART and DTEND instead of the device timezone`() {
        // Asia/Tokyo (+09:00, no DST) is deliberately different from any plausible
        // CI/dev machine default timezone so this test can't pass by coincidence.
        val icsContent =
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:event3@kairos
            DTSTART;TZID=Asia/Tokyo:20260603T103000
            DTEND;TZID=Asia/Tokyo:20260603T113000
            SUMMARY:Reuniao com fuso horario
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()

        val events = IcsParser.parseIcs(icsContent)

        assertEquals(1, events.size)
        val event = events[0]

        val format =
            SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            }
        val expectedStart = format.parse("20260603T103000")?.time
        val expectedEnd = format.parse("20260603T113000")?.time

        assertEquals(expectedStart, event.startTime)
        assertEquals(expectedEnd, event.endTime)
    }

    @Test
    fun `events without a UID still get an id and escaped text is unescaped`() {
        val ics =
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Café\, pão
            LOCATION:Rua A\, 10
            DTSTART:20240101T100000Z
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()

        val event = IcsParser.parseIcs(ics).single()

        assertTrue(event.id > 0)
        assertEquals("Café, pão", event.title)
        assertEquals("Rua A, 10", event.location)
    }

    @Test
    fun `events with an unreadable start or no title are skipped`() {
        val ics =
            """
            BEGIN:VEVENT
            SUMMARY:Broken date
            DTSTART:not-a-date
            END:VEVENT
            BEGIN:VEVENT
            DTSTART:20240101T100000Z
            END:VEVENT
            END:VEVENT
            BEGIN:VEVENT
            A line without a colon
            SUMMARY:Ok
            DTSTART:20240102T100000Z
            END:VEVENT
            """.trimIndent()

        val events = IcsParser.parseIcs(ics)

        assertEquals(listOf("Ok"), events.map { it.title })
    }
}
