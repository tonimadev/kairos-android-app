package digital.tonima.kairos.ui.components

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DayStripTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.now()
    private val todayEpoch = today.toEpochDay()
    private val thisMonth = YearMonth.from(today).atDay(1).toEpochDay()

    @Test
    fun `the window covers the days around today`() {
        val range = dayStripRange(todayEpoch, todayEpoch)

        assertTrue(todayEpoch - 14 in range)
        assertTrue(todayEpoch + 60 in range)
    }

    @Test
    fun `a day picked far from today stays inside the window`() {
        val far = todayEpoch + 400

        assertTrue(far in dayStripRange(todayEpoch, far))
        assertTrue(todayEpoch in dayStripRange(todayEpoch, far))
    }

    @Test
    fun `a day of the loaded month does not reload the month`() {
        val loaded = LocalDate.of(2026, 10, 1).toEpochDay()

        assertNull(monthToLoadFor(LocalDate.of(2026, 10, 20).toEpochDay(), loaded))
    }

    @Test
    fun `a day of another month asks for that month to be loaded`() {
        val loaded = LocalDate.of(2026, 10, 1).toEpochDay()

        assertEquals(
            LocalDate.of(2026, 11, 1).toEpochDay(),
            monthToLoadFor(LocalDate.of(2026, 11, 2).toEpochDay(), loaded),
        )
    }

    @Test
    fun `tapping a day selects it`() {
        val selected = mutableListOf<Long>()
        // Two days before the end of the month, so the tapped day is not the 1st (which shows the month name).
        val start = YearMonth.from(today).atDay(10).toEpochDay()
        compose.setContent {
            DayStrip(
                selectedDate = start,
                currentMonth = thisMonth,
                onMonthChanged = {},
                onDateSelected = { selected += it },
            )
        }

        compose.onNodeWithContentDescription(fullDate(LocalDate.ofEpochDay(start + 1))).performClick()

        assertEquals(listOf(start + 1), selected)
    }

    @Test
    fun `tapping a day of another month loads that month before selecting it`() {
        val calls = mutableListOf<String>()
        val endOfMonth = YearMonth.from(today).atEndOfMonth()
        val firstOfNext = endOfMonth.plusDays(1)
        compose.setContent {
            DayStrip(
                selectedDate = endOfMonth.toEpochDay(),
                currentMonth = thisMonth,
                onMonthChanged = { calls += "month:$it" },
                onDateSelected = { calls += "date:$it" },
            )
        }

        compose.onNodeWithContentDescription(fullDate(firstOfNext)).performClick()

        assertEquals(listOf("month:${firstOfNext.toEpochDay()}", "date:${firstOfNext.toEpochDay()}"), calls)
    }

    private fun fullDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()))
}
