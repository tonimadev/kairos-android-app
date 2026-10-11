package digital.tonima.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class WeekendDaysTest {
    @Test
    fun `Brazil and the United States rest on Saturday and Sunday`() {
        assertEquals(setOf(SATURDAY, SUNDAY), WeekendDays.forLocale(Locale.forLanguageTag("pt-BR")))
        assertEquals(setOf(SATURDAY, SUNDAY), WeekendDays.forLocale(Locale.forLanguageTag("en-US")))
    }

    @Test
    fun `Algeria rests on Friday and Saturday`() {
        assertEquals(setOf(FRIDAY, SATURDAY), WeekendDays.forLocale(Locale.forLanguageTag("ar-DZ")))
    }

    @Test
    fun `Iran rests on Friday and works on Saturday and Sunday`() {
        val weekend = WeekendDays.forLocale(Locale.forLanguageTag("fa-IR"))

        assertTrue(FRIDAY in weekend)
        assertFalse(SATURDAY in weekend)
        assertFalse(SUNDAY in weekend)
    }

    @Test
    fun `a locale without a region uses the most likely region of its language`() {
        assertEquals(
            WeekendDays.forLocale(Locale.forLanguageTag("fa-IR")),
            WeekendDays.forLocale(Locale.forLanguageTag("fa")),
        )
    }
}
