package digital.tonima.kairos.ui.view

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.AlarmOffset
import digital.tonima.kairos.core.model.DeviceCalendar
import digital.tonima.kairos.ui.components.SettingsActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val calls = mutableListOf<Pair<String, Any?>>()

    private val actions =
        SettingsActions(
            onToggle = { calls += "global" to it },
            onDismissAutostart = {},
            onVibrateToggle = { calls += "vibrate" to it },
            onAllDayAlarmsToggle = { calls += "allDay" to it },
            onAllDayAlarmHourChanged = { calls += "allDayHour" to it },
            onAlarmOffsetChanged = { calls += "offset" to it },
            onSnoozeTimeChanged = { calls += "snooze" to it },
            onSkipWeekendsToggle = { calls += "skipWeekends" to it },
            onAutoDismissMinutesChanged = { calls += "autoDismiss" to it },
            onCalendarFilterToggle = { id, enabled -> calls += "calendar" to (id to enabled) },
            onLocationAlarmToggle = { calls += "location" to it },
            onTransportModeChanged = { calls += "transport" to it },
            onTemperatureUnitToggle = { calls += "celsius" to it },
            onCloseSettings = { calls += "close" to null },
        )

    // region Switches

    @Test
    fun `switches reflect the saved settings`() {
        render(SettingsUiState(vibrateOnly = true, skipWeekends = false, isTemperatureInCelsius = true))

        switchFor(R.string.vibrate_only).assertIsOn()
        switchFor(R.string.skip_weekends_label).assertIsOff()
        switchFor(R.string.use_celsius_for_weather).assertIsOn()
    }

    @Test
    fun `each switch reports its own setting`() {
        render(SettingsUiState(vibrateOnly = false, skipWeekends = false, allDayAlarmsEnabled = true))

        switchFor(R.string.vibrate_only).performClick()
        switchFor(R.string.skip_weekends_label).performClick()
        switchFor(R.string.all_day_alarms).performScrollTo().performClick()
        switchFor(R.string.use_celsius_for_weather).performScrollTo().performClick()

        assertEquals(
            listOf("vibrate" to true, "skipWeekends" to true, "allDay" to false, "celsius" to false),
            calls,
        )
    }

    // endregion

    // region All-day alarms

    @Test
    fun `all-day alarm hour is only shown while all-day alarms are enabled`() {
        render(SettingsUiState(allDayAlarmsEnabled = false))
        compose.onAllNodesWithText(string(R.string.all_day_alarm_time), substring = true).assertCountEquals(0)
    }

    @Test
    fun `moving the all-day hour slider reports the rounded hour`() {
        render(SettingsUiState(allDayAlarmsEnabled = true, allDayAlarmHour = 9))

        sliderLabelled(
            R.string.all_day_alarm_time,
        ).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {
            it(7.4f)
        }

        assertEquals(listOf("allDayHour" to 7), calls)
    }

    // endregion

    // region Offset

    @Test
    fun `alarm offset dropdown shows the current offset and reports the chosen one`() {
        render(SettingsUiState(alarmOffsetMinutes = 0L))

        compose.onNodeWithText(string(R.string.alarm_offset_at_time)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.alarm_offset_30_min)).performClick()

        assertEquals(listOf("offset" to AlarmOffset.THIRTY_MINUTES), calls)
    }

    // endregion

    // region Sliders

    @Test
    fun `snooze and auto dismiss sliders report whole minutes`() {
        render(SettingsUiState(snoozeTimeMinutes = 10, autoDismissMinutes = 10))

        sliderLabelled(
            R.string.snooze_time_label,
        ).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {
            it(15.2f)
        }
        sliderLabelled(
            R.string.auto_dismiss_label,
        ).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {
            it(5.6f)
        }

        assertEquals(listOf("snooze" to 15, "autoDismiss" to 6), calls)
    }

    // endregion

    // region Location alarm

    @Test
    fun `location alarm explains it is an ai feature for free users`() {
        render(SettingsUiState(isLocationAlarmEnabled = false), isAiUser = false)

        compose.onNodeWithText(string(R.string.geo_alarm_pro_only)).performScrollTo().assertExists()
        switchFor(R.string.location_alarm_title).assertIsOff()
    }

    @Test
    fun `location alarm is shown as off for free users even if it was enabled before`() {
        render(SettingsUiState(isLocationAlarmEnabled = true), isAiUser = false)

        switchFor(R.string.location_alarm_title).performScrollTo().assertIsOff()
    }

    @Test
    fun `ai users can pick the transport mode once the location alarm is on`() {
        render(SettingsUiState(isLocationAlarmEnabled = true, preferredTransportMode = "driving"), isAiUser = true)

        compose.onNodeWithText(string(R.string.transport_driving)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.transport_transit)).performClick()

        assertEquals(listOf("transport" to "transit"), calls)
    }

    @Test
    fun `transport mode is hidden while the location alarm is off`() {
        render(SettingsUiState(isLocationAlarmEnabled = false), isAiUser = true)

        compose.onAllNodesWithText(string(R.string.transport_mode_label)).assertCountEquals(0)
    }

    // endregion

    // region Calendar filter

    @Test
    fun `calendar filter lists calendars and reports toggles`() {
        val work = DeviceCalendar(id = 7L, displayName = "Work", accountName = "me@company.com")
        val personal = DeviceCalendar(id = 8L, displayName = "", accountName = "me@gmail.com")
        render(
            SettingsUiState(),
            calendars = listOf(work, personal),
            enabledCalendarIds = setOf(7L),
        )

        switchFor("Work").performScrollTo().assertIsOn()
        // A calendar without a display name falls back to its account name.
        switchFor("me@gmail.com").performScrollTo().assertIsOff().performClick()

        assertEquals(listOf("calendar" to (8L to true)), calls)
    }

    @Test
    fun `with no calendar filter every calendar is enabled`() {
        render(
            SettingsUiState(),
            calendars = listOf(DeviceCalendar(id = 7L, displayName = "Work", accountName = "a")),
            enabledCalendarIds = emptySet(),
        )

        switchFor("Work").performScrollTo().assertIsOn()
    }

    @Test
    fun `calendar filter is hidden when there are no calendars`() {
        render(SettingsUiState(), calendars = emptyList())

        compose.onAllNodesWithText(string(R.string.calendar_filter_title)).assertCountEquals(0)
    }

    // endregion

    @Test
    fun `back button closes the settings`() {
        render(SettingsUiState())

        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertEquals(listOf("close" to null), calls)
    }

    private fun render(
        settings: SettingsUiState,
        isAiUser: Boolean = true,
        calendars: List<DeviceCalendar> = emptyList(),
        enabledCalendarIds: Set<Long> = emptySet(),
    ) {
        compose.setContent {
            SettingsScreen(
                uiState =
                    EventScreenUiState(
                        isAiUser = isAiUser,
                        availableCalendars = calendars,
                        enabledCalendarIds = enabledCalendarIds,
                    ),
                settingsUiState = settings,
                settingsActions = actions,
            )
        }
    }

    /** The switch on the same line as [label]: rows add no semantics nodes, so match by position. */
    private fun switchFor(label: String): SemanticsNodeInteraction {
        val labelSpan = verticalSpan(compose.onAllNodesWithText(label)[0].fetchSemanticsNode())
        val switches = compose.onAllNodes(isToggleable()).fetchSemanticsNodes()
        val index =
            switches.indices.maxBy { i ->
                val span = verticalSpan(switches[i])
                minOf(span.endInclusive, labelSpan.endInclusive) - maxOf(span.start, labelSpan.start)
            }
        return compose.onAllNodes(isToggleable())[index]
    }

    private fun switchFor(labelRes: Int) = switchFor(string(labelRes))

    /** The slider right below the text that labels it ("<label>: <value>"). */
    private fun sliderLabelled(labelRes: Int): SemanticsNodeInteraction {
        val slider = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
        val labelBottom =
            verticalSpan(compose.onAllNodesWithText(string(labelRes), substring = true)[0].fetchSemanticsNode())
                .endInclusive
        val sliders = compose.onAllNodes(slider).fetchSemanticsNodes()
        val index =
            sliders.indices
                .filter { verticalSpan(sliders[it]).start >= labelBottom - 1f }
                .minBy { verticalSpan(sliders[it]).start - labelBottom }
        return compose.onAllNodes(slider)[index]
    }

    /** Unclipped vertical extent: boundsInRoot is clipped to the scroll viewport for off-screen rows. */
    private fun verticalSpan(node: SemanticsNode): ClosedFloatingPointRange<Float> =
        node.positionInRoot.y..(node.positionInRoot.y + node.size.height)

    private fun string(resId: Int) = app.getString(resId)
}
