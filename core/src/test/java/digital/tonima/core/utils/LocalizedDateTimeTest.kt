package digital.tonima.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LocalizedDateTimeTest {
    private val moment = LocalDateTime.of(2026, 10, 5, 15, 30)
    private val ptBr = Locale("pt", "BR")

    private fun String.plain() = replace(' ', ' ').replace(' ', ' ')

    @Test
    fun `numeric dates follow the order of the locale`() {
        val day = LocalDate.of(2026, 10, 5)

        assertEquals("05/10/2026", day.format(LocalizedDateTime.numericDate(ptBr)))
        assertEquals("10/5/2026", day.format(LocalizedDateTime.numericDate(Locale.US)))
        assertTrue(day.format(LocalizedDateTime.numericDate(Locale.JAPAN)).startsWith("2026"))
    }

    @Test
    fun `time follows the 24 hour setting`() {
        assertEquals("15:30", moment.format(LocalizedDateTime.time(Locale.US, is24Hour = true)))
        assertEquals("3:30 PM", moment.format(LocalizedDateTime.time(Locale.US, is24Hour = false)).plain())
    }

    @Test
    fun `a 24 hour clock never shows am or pm`() {
        listOf(ptBr, Locale.GERMANY, Locale.FRANCE, Locale.JAPAN, Locale("ru", "RU")).forEach {
            val text = moment.format(LocalizedDateTime.time(it, is24Hour = true))
            assertTrue("$it -> $text", text.contains("15") && !text.contains("PM", ignoreCase = true))
        }
    }

    @Test
    fun `weekday and time use the locale weekday name`() {
        val text = moment.format(LocalizedDateTime.weekdayAndTime(ptBr, is24Hour = true))

        assertTrue(text, text.contains("seg", ignoreCase = true) && text.contains("15:30"))
    }

    @Test
    fun `date and time combine both`() {
        val text = moment.format(LocalizedDateTime.numericDateAndTime(ptBr, is24Hour = true))

        assertTrue(text, text.contains("05/10/2026") && text.contains("15:30"))
    }
}
