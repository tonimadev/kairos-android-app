package digital.tonima.kairos.ui.view

import android.app.Application
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.viewmodel.AiSideEffect.RequireUserConfirmation
import digital.tonima.core.viewmodel.AiUiState
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.core.viewmodel.UiText
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.AiActions
import digital.tonima.kairos.ui.components.EventActions
import digital.tonima.kairos.ui.components.MainContent
import digital.tonima.kairos.ui.components.SettingsActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "en-rUS-w411dp-h891dp")
class MainScreenPartsTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val calls = mutableListOf<String>()
    private val monthTitle = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))

    // region MainContent

    @Test
    fun `on phones the calendar dashboard is collapsed until requested`() {
        mainContent(windowSizeClass = null)
        compose.onAllNodesWithText(monthTitle).assertCountEquals(0)

        compose.onNodeWithText(string(R.string.show_dashboard)).performClick()

        compose.onNodeWithText(monthTitle).assertExists()
        compose.onNodeWithText(string(R.string.hide_dashboard)).performClick()
        compose.onAllNodesWithText(string(R.string.show_dashboard)).assertCountEquals(1)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun `on large screens the calendar is always shown next to the list`() {
        mainContent(windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1200.dp, 800.dp)))

        compose.onNodeWithText(monthTitle).assertExists()
        compose.onAllNodesWithText(string(R.string.show_dashboard)).assertCountEquals(0)
    }

    private fun mainContent(windowSizeClass: WindowSizeClass?) {
        compose.setContent {
            MainContent(
                uiState = EventScreenUiState(isAiUser = true),
                settingsUiState = SettingsUiState(),
                aiUiState = AiUiState(),
                eventActions =
                    EventActions(
                        onRefresh = {},
                        onEventToggle = { _, _, _ -> },
                        onEventVibrateToggle = { _, _ -> },
                        onMonthChanged = {},
                        onDateSelected = {},
                        onEventClick = {},
                        onReturnToToday = {},
                    ),
                settingsActions =
                    SettingsActions(
                        onToggle = {},
                        onDismissAutostart = {},
                        onVibrateToggle = {},
                        onAllDayAlarmsToggle = {},
                        onAllDayAlarmHourChanged = {},
                        onAlarmOffsetChanged = {},
                    ),
                aiActions = AiActions(),
                windowSizeClass = windowSizeClass,
            )
        }
    }

    // endregion

    // region EventScreenDialogs

    @Test
    fun `ai actions need explicit confirmation`() {
        dialogs(aiConfirmation = confirmation)

        compose.onNodeWithText("Delete event?").assertExists()
        compose.onNodeWithText("The dentist appointment will be removed.").assertExists()
        compose.onNodeWithText(string(R.string.confirm)).performClick()

        assertEquals(listOf("clear", "approve"), calls)
    }

    @Test
    fun `cancelling an ai action rejects it`() {
        dialogs(aiConfirmation = confirmation)

        compose.onNodeWithText(string(R.string.cancel)).performClick()

        assertEquals(listOf("clear", "reject"), calls)
    }

    @Test
    fun `rating sheet offers rate now, later and never`() {
        dialogs(showRating = true)

        compose.onNodeWithText(string(R.string.rate_app_title)).assertExists()
        compose.onNodeWithText(string(R.string.rate_now)).performClick()
        compose.onNodeWithText(string(R.string.rate_later)).performClick()
        compose.onNodeWithText(string(R.string.rate_never)).performClick()

        assertEquals(listOf("rateNow", "rateLater", "rateNever"), calls)
    }

    @Test
    fun `no dialog is shown without a pending action or rating prompt`() {
        dialogs()

        compose.onAllNodesWithText(string(R.string.confirm)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.rate_now)).assertCountEquals(0)
    }

    private val confirmation =
        RequireUserConfirmation(
            title = UiText.DynamicString("Delete event?"),
            message = UiText.DynamicString("The dentist appointment will be removed."),
        )

    private fun dialogs(
        aiConfirmation: RequireUserConfirmation? = null,
        showRating: Boolean = false,
    ) {
        compose.setContent {
            EventScreenDialogs(
                uiState = EventScreenUiState(showRatingBottomSheet = showRating),
                aiConfirmationData = aiConfirmation,
                onClearAiConfirmation = { calls += "clear" },
                onRateNow = { calls += "rateNow" },
                onRateLater = { calls += "rateLater" },
                onRateNeverShow = { calls += "rateNever" },
                onApproveAiAction = { calls += "approve" },
                onRejectAiAction = { calls += "reject" },
            )
        }
    }

    // endregion

    private fun string(resId: Int) = app.getString(resId)
}
