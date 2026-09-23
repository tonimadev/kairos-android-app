package digital.tonima.kairos.wear.ui.components

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.core.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "w227dp-h227dp-round")
class WearComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val toggles = mutableListOf<Triple<Long, Boolean, Boolean>>()

    private val dentist =
        EventUiModel(
            id = 1L,
            title = "Dentist",
            startTime = 1_800_000_000_000L,
            location = "Rua Augusta",
            isAlarmEnabled = true,
            travelTimeMinutes = 25,
        )
    private val standup = EventUiModel(id = 2L, title = "Standup", startTime = 1_800_010_000_000L, isRecurring = true)

    @Test
    fun `empty day shows the no events message`() {
        list(emptyList())

        compose.onNodeWithText(string(R.string.no_events_found_for_this_day)).assertExists()
    }

    @Test
    fun `event cards show location, travel time and recurrence`() {
        list(listOf(dentist, standup))

        compose.onNodeWithText("Dentist").assertExists()
        compose.onNodeWithText("📍 Rua Augusta").assertExists()
        compose.onNodeWithText("🚗 " + app.getString(R.string.minutes_short, 25)).assertExists()
        compose.onNodeWithText("🔁 " + string(R.string.recurring_label)).assertExists()
    }

    @Test
    fun `tapping a single event toggles its alarm directly`() {
        list(listOf(dentist))

        compose.onNodeWithText("Dentist").performClick()

        assertEquals(listOf(Triple(1L, false, false)), toggles)
    }

    @Test
    fun `tapping a recurring event asks whether to apply to the whole series`() {
        list(listOf(standup))

        compose.onNodeWithText("Standup").performClick()
        assertTrue(toggles.isEmpty())
        compose.onNodeWithText(string(R.string.update_alarm_title)).assertExists()
        compose.onNodeWithText(string(R.string.recurring_option)).performClick()

        assertEquals(listOf(Triple(2L, true, true)), toggles)
        compose.onAllNodesWithText(string(R.string.update_alarm_title)).assertCountEquals(0)
    }

    @Test
    fun `recurring toggle can apply to this occurrence only`() {
        list(listOf(standup))

        compose.onNodeWithText("Standup").performClick()
        compose.onNodeWithText(string(R.string.only_this_option)).performClick()

        assertEquals(listOf(Triple(2L, true, false)), toggles)
    }

    @Test
    fun `permission screen offers settings and retry`() {
        val calls = mutableListOf<String>()
        compose.setContent {
            WearOsPermissionsScreenContent(
                onSettingsClick = { calls += "settings" },
                onRetryClick = { calls += "retry" },
            )
        }

        compose.onNodeWithText(string(R.string.open_settings)).performClick()
        compose.onNodeWithText(string(R.string.provide_permission)).performClick()

        assertEquals(listOf("settings", "retry"), calls)
    }

    private fun list(events: List<EventUiModel>) {
        compose.setContent {
            EventsListSection(
                events = events,
                isRefreshing = false,
                isGlobalAlarmEnabled = true,
                onEventToggle = { event, enabled, series -> toggles += Triple(event.id, enabled, series) },
            )
        }
    }

    private fun string(resId: Int) = app.getString(resId)
}
