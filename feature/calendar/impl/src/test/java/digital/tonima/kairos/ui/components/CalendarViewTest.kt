package digital.tonima.kairos.ui.components

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.core.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "en-rUS")
class CalendarViewTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val thisMonth = YearMonth.now()
    private val selected = mutableListOf<Long>()
    private val monthChanges = mutableListOf<Long>()
    private var returnedToToday = 0

    @Test
    fun `header shows the visible month`() {
        render(month = thisMonth)

        compose.onNodeWithText(thisMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))).assertExists()
    }

    @Test
    fun `tapping a day of the month selects it`() {
        render(month = thisMonth)

        // Day 15 is always inside the visible month, never a leading/trailing day of a neighbour.
        compose.onNode(hasText("15") and hasClickAction() and isEnabled()).performClick()

        assertEquals(listOf(thisMonth.atDay(15).toEpochDay()), selected)
    }

    @Test
    fun `the back to today button only appears away from the current month`() {
        render(month = thisMonth)
        compose.onAllNodesWithContentDescription(string(R.string.back_to_today)).assertCountEquals(0)
    }

    @Test
    fun `back to today is offered when browsing another month`() {
        val other = thisMonth.plusMonths(3)
        render(month = other)

        compose.onNodeWithText(other.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))).assertExists()
        compose.onNodeWithContentDescription(string(R.string.back_to_today)).performClick()

        assertEquals(1, returnedToToday)
    }

    @Test
    fun `showing the requested month does not report a month change`() {
        render(month = thisMonth.plusMonths(2))

        assertTrue(monthChanges.isEmpty())
    }

    @Test
    fun `days with events stay selectable`() {
        val day = thisMonth.atDay(15).toEpochDay()
        render(
            month = thisMonth,
            events = ImmutableMap.of(day, ImmutableList.of(EventUiModel(id = 1L, title = "x", startTime = 0L))),
        )

        compose.onNode(hasText("15") and hasClickAction() and isEnabled()).performClick()

        assertEquals(listOf(day), selected)
    }

    private fun render(
        month: YearMonth,
        events: ImmutableMap<Long, ImmutableList<EventUiModel>> = ImmutableMap.of(),
    ) {
        compose.setContent {
            CalendarView(
                currentMonth = month.atDay(1).toEpochDay(),
                selectedDate = month.atDay(1).toEpochDay(),
                eventsByDate = events,
                onMonthChanged = { monthChanges += it },
                onDateSelected = { selected += it },
                onReturnToToday = { returnedToToday++ },
            )
        }
    }

    private fun string(resId: Int) = app.getString(resId)
}
