package digital.tonima.core.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarNotificationMatcherTest {
    @Test
    fun `matches when the notification title is the event title`() {
        assertTrue(CalendarNotificationMatcher.isSameEvent(listOf("Daily Standup", "10:00 – 10:15"), "Daily standup"))
    }

    @Test
    fun `ignores case and accents`() {
        assertTrue(CalendarNotificationMatcher.isSameEvent(listOf("REUNIÃO de produto"), "reuniao de produto"))
    }

    @Test
    fun `matches when only the body mentions the event`() {
        assertTrue(CalendarNotificationMatcher.isSameEvent(listOf("Lembrete", "Dentista às 15:00"), "Dentista"))
    }

    @Test
    fun `does not match a different event`() {
        assertFalse(CalendarNotificationMatcher.isSameEvent(listOf("Almoço com a equipe"), "Dentista"))
    }

    @Test
    fun `very short titles never match to avoid dismissing unrelated reminders`() {
        assertFalse(CalendarNotificationMatcher.isSameEvent(listOf("Go to the gym"), "Go"))
    }

    @Test
    fun `null texts are tolerated`() {
        assertFalse(CalendarNotificationMatcher.isSameEvent(listOf(null, null), "Dentista"))
    }
}
