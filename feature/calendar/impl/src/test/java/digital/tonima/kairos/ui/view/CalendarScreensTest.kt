package digital.tonima.kairos.ui.view

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.google.common.collect.ImmutableList
import digital.tonima.core.data.usecases.DeleteCalendarUseCase
import digital.tonima.core.data.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.data.usecases.UpdateCalendarUseCase
import digital.tonima.core.usecases.ImportIcsUseCase
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.ImportCalendarViewModel
import digital.tonima.core.viewmodel.ManageCalendarsViewModel
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.DeviceCalendar
import digital.tonima.kairos.core.model.InsightsPeriod
import digital.tonima.kairos.ui.components.InsightsContent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CalendarScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    // region InsightsContent

    @Test
    fun `insights show streak, snoozes and the chosen period`() {
        val periods = mutableListOf<InsightsPeriod>()
        compose.setContent {
            InsightsContent(
                uiState =
                    EventScreenUiState(
                        currentStreak = 4,
                        snoozeCount = 7,
                        punctualityScore = 90,
                        meetingStats = ImmutableList.of("Mon" to 1.5f, "Tue" to 2f),
                    ),
                onPeriodChange = { periods += it },
            )
        }

        compose.onNodeWithText(app.getString(R.string.insights_streak_days, 4)).assertExists()
        compose.onNodeWithText("7").assertExists()
        compose.onNodeWithText(string(R.string.insights_punctuality_great)).performScrollTo().assertExists()
        compose.onNodeWithText(string(R.string.insights_period_month)).performScrollTo().performClick()

        assertEquals(listOf(InsightsPeriod.MONTH), periods)
    }

    @Test
    fun `low punctuality and empty meeting stats are explained`() {
        compose.setContent {
            InsightsContent(uiState = EventScreenUiState(punctualityScore = 40), onPeriodChange = {})
        }

        compose.onNodeWithText(string(R.string.insights_punctuality_bad)).performScrollTo().assertExists()
        compose.onNodeWithText(string(R.string.insights_no_data)).performScrollTo().assertExists()
    }

    @Test
    fun `ai usage is only shown to ai users`() {
        compose.setContent {
            InsightsContent(uiState = EventScreenUiState(isAiUser = false), onPeriodChange = {})
        }

        compose.onAllNodesWithText(string(R.string.insights_ai_usage)).assertCountEquals(0)
    }

    // endregion

    // region ManageCalendarsScreen

    private val getCalendars: GetAvailableCalendarsUseCase = mockk()
    private val updateCalendar: UpdateCalendarUseCase = mockk()
    private val deleteCalendar: DeleteCalendarUseCase = mockk()
    private val imported = DeviceCalendar(id = 10L, displayName = "Feriados", accountName = "Kairos Imports", color = 1)

    @Test
    fun `manage calendars lists imported calendars and deletes one`() {
        coEvery { getCalendars() } returnsMany listOf(listOf(imported), emptyList())
        coEvery { deleteCalendar(imported.id) } returns true
        renderManageCalendars()

        compose.onNodeWithText("Feriados").assertExists()
        compose.onNodeWithContentDescription("Deletar").performClick()

        coVerify { deleteCalendar(imported.id) }
        compose.onNodeWithText("Nenhum calendário importado encontrado.").assertExists()
    }

    @Test
    fun `manage calendars edits the name of a calendar`() {
        coEvery { getCalendars() } returns listOf(imported)
        coEvery { updateCalendar(imported.id, "Feriados BR", any()) } returns true
        renderManageCalendars()

        compose.onNodeWithContentDescription("Editar").performClick()
        compose.onNode(hasSetTextAction() and hasText("Feriados")).performTextReplacement("Feriados BR")
        compose.onNodeWithText("Salvar").performClick()

        coVerify { updateCalendar(imported.id, "Feriados BR", 1) }
    }

    @Test
    fun `manage calendars back button navigates back`() {
        coEvery { getCalendars() } returns emptyList()
        var back = 0
        renderManageCalendars(onBack = { back++ })

        compose.onNodeWithContentDescription("Voltar").performClick()

        assertEquals(1, back)
    }

    private fun renderManageCalendars(onBack: () -> Unit = {}) {
        val viewModel = ManageCalendarsViewModel(getCalendars, updateCalendar, deleteCalendar)
        compose.setContent { ManageCalendarsScreen(viewModel = viewModel, onNavigateBack = onBack) }
    }

    // endregion

    // region ImportCalendarScreen

    private val importIcs: ImportIcsUseCase = mockk()

    @Test
    // The import form does not scroll, so its button only fits on a regular-size phone screen.
    @Config(qualifiers = "w411dp-h891dp")
    fun `import screen explains a missing name`() {
        renderImport()

        compose.onNodeWithText(string(R.string.import_calendar_submit)).performClick()

        compose.onNodeWithText(string(R.string.import_calendar_error_name_required)).assertExists()
        compose.onNodeWithText(string(R.string.ok)).performClick()
        compose.onAllNodesWithText(string(R.string.import_calendar_error_name_required)).assertCountEquals(0)
    }

    @Test
    fun `import screen collects name and url and toggles the source`() {
        val viewModel = renderImport()

        compose.onNodeWithText(string(R.string.import_calendar_name_label)).performTextInput("Feriados")
        compose.onNodeWithText(string(R.string.import_calendar_url_label)).performTextInput("https://x/cal.ics")
        compose.onNodeWithText(string(R.string.import_calendar_enable_alarms)).assertExists()

        assertEquals("Feriados", viewModel.uiState.value.calendarName)
        assertEquals("https://x/cal.ics", viewModel.uiState.value.url)
        compose.onNodeWithText(string(R.string.import_calendar_select_file)).assertExists()
    }

    @Test
    fun `import screen back button navigates back`() {
        var back = 0
        renderImport(onBack = { back++ })

        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertEquals(1, back)
    }

    private fun renderImport(onBack: () -> Unit = {}): ImportCalendarViewModel {
        val viewModel = ImportCalendarViewModel(importIcs, app)
        compose.setContent { ImportCalendarScreen(viewModel = viewModel, onNavigateBack = onBack) }
        return viewModel
    }

    // endregion

    private fun string(resId: Int) = app.getString(resId)
}
