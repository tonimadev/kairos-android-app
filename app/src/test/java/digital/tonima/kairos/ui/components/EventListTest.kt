package digital.tonima.kairos.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import digital.tonima.core.viewmodel.AiUiState
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.core.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EventListTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.now().toEpochDay()
    private val toggles = mutableListOf<Triple<EventUiModel, Boolean, Boolean>>()
    private val clicked = mutableListOf<EventUiModel>()
    private val searches = mutableListOf<String>()
    private var upgradeRequests = 0

    private val single = event(id = 1L, title = "Dentist", hour = 10)
    private val weekly = event(id = 2L, title = "Weekly sync", hour = 14, isRecurring = true)

    @Test
    fun `shows the events of the selected day`() {
        render(events = listOf(single, weekly))

        compose.onNodeWithText("Dentist").assertExists()
        compose.onNodeWithText("Weekly sync").assertExists()
    }

    @Test
    fun `shows the empty state when the day has no events`() {
        render(events = emptyList())

        compose.onNodeWithText(string(R.string.no_alarms_found)).assertExists()
    }

    @Test
    fun `search filters events by title ignoring case`() {
        render(events = listOf(single, weekly), searchQuery = "WEEKLY")

        compose.onNodeWithText("Weekly sync").assertExists()
        compose.onNodeWithText("Dentist").assertDoesNotExist()
    }

    @Test
    fun `typing in the search field reports the query`() {
        render(events = listOf(single))

        compose.onNodeWithText(string(R.string.search)).performTextInput("den")

        assertEquals(listOf("den"), searches)
    }

    @Test
    fun `clearing the search reports an empty query`() {
        render(events = listOf(single), searchQuery = "den")

        compose.onNodeWithContentDescription(string(R.string.clear_search)).performClick()

        assertEquals(listOf(""), searches)
    }

    @Test
    fun `toggling a single event applies immediately to that occurrence`() {
        render(events = listOf(single.copy(isAlarmEnabled = true)))

        compose.onNode(isToggleable()).assertIsOn().performClick()

        assertEquals(listOf(Triple(single.copy(isAlarmEnabled = true), false, false)), toggles)
    }

    @Test
    fun `toggling a recurring event asks whether to change all occurrences`() {
        render(events = listOf(weekly))

        compose.onNode(isToggleable()).assertIsOff().performClick()
        assertTrue("Nothing may change before the user chooses", toggles.isEmpty())
        compose.onNodeWithText(string(R.string.update_alarm_title)).assertExists()

        compose.onNodeWithText(string(R.string.recurring_option)).performClick()

        assertEquals(listOf(Triple(weekly, true, true)), toggles)
        compose.onNodeWithText(string(R.string.update_alarm_title)).assertDoesNotExist()
    }

    @Test
    fun `choosing only this occurrence keeps the rest of the series`() {
        render(events = listOf(weekly))

        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText(string(R.string.only_this_option)).performClick()

        assertEquals(listOf(Triple(weekly, true, false)), toggles)
    }

    @Test
    fun `tapping an event opens it`() {
        render(events = listOf(single))

        compose.onNodeWithText("Dentist").performClick()

        assertEquals(listOf(single), clicked)
    }

    @Test
    fun `free users see the upgrade card and can request the subscription`() {
        render(events = emptyList(), isAiUser = false)

        compose.onNodeWithText(string(R.string.try_pro_plan)).performClick()

        assertEquals(1, upgradeRequests)
    }

    private fun render(
        events: List<EventUiModel>,
        searchQuery: String = "",
        isAiUser: Boolean = true,
    ) {
        compose.setContent {
            EventList(
                uiState =
                    EventScreenUiState(
                        selectedDate = today,
                        searchQuery = searchQuery,
                        isAiUser = isAiUser,
                    ),
                aiUiState = AiUiState(),
                settingsUiState = SettingsUiState(isGlobalAlarmEnabled = true),
                eventsByDate = ImmutableMap.of(today, ImmutableList.copyOf(events)),
                eventActions =
                    EventActions(
                        onRefresh = {},
                        onEventToggle = { event, enabled, all -> toggles += Triple(event, enabled, all) },
                        onEventVibrateToggle = { _, _ -> },
                        onMonthChanged = {},
                        onDateSelected = {},
                        onEventClick = { clicked += it },
                        onReturnToToday = {},
                        onSearchQueryChanged = { searches += it },
                    ),
                aiActions = AiActions(onSubscriptionRequest = { upgradeRequests++ }),
            )
        }
    }

    private fun event(
        id: Long,
        title: String,
        hour: Int,
        isRecurring: Boolean = false,
    ): EventUiModel {
        val start = LocalDate.now().atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return EventUiModel(
            id = id,
            title = title,
            startTime = start,
            endTime = start + 3_600_000L,
            isRecurring = isRecurring,
        )
    }

    private fun string(resId: Int) = app.getString(resId)
}
