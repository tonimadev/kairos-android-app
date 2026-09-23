package digital.tonima.core.viewmodel

import android.content.Context
import digital.tonima.core.usecases.ImportIcsException
import digital.tonima.core.usecases.ImportIcsUseCase
import digital.tonima.kairos.core.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImportCalendarViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val importIcs: ImportIcsUseCase = mockk()
    private lateinit var icsFile: File

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        icsFile = tempFolder.newFile("holidays.ics").apply { writeText(ICS) }
        coEvery { importIcs(any(), any(), any(), any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Validation

    @Test
    fun `import requires a calendar name`() {
        val viewModel = viewModel()
        viewModel.handleIntent(ImportCalendarIntent.UpdateUrl(icsFile.toURI().toString()))

        viewModel.handleIntent(ImportCalendarIntent.SubmitImport)

        assertEquals(message(R.string.import_calendar_error_name_required), viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 0) { importIcs(any(), any(), any(), any()) }
    }

    @Test
    fun `import requires a url or a file`() {
        val viewModel = viewModel()
        viewModel.handleIntent(ImportCalendarIntent.UpdateName("Feriados"))

        viewModel.handleIntent(ImportCalendarIntent.SubmitImport)

        assertEquals(message(R.string.import_calendar_error_source_required), viewModel.uiState.value.error)
        coVerify(exactly = 0) { importIcs(any(), any(), any(), any()) }
    }

    @Test
    fun `a blank name is rejected`() {
        val viewModel = viewModel()
        viewModel.handleIntent(ImportCalendarIntent.UpdateName("   "))
        viewModel.handleIntent(ImportCalendarIntent.UpdateUrl(icsFile.toURI().toString()))

        viewModel.handleIntent(ImportCalendarIntent.SubmitImport)

        assertNotNull(viewModel.uiState.value.error)
    }

    // endregion

    // region Source selection

    @Test
    fun `choosing a file clears the url and vice versa`() {
        val viewModel = viewModel()

        viewModel.handleIntent(ImportCalendarIntent.UpdateUrl("https://example.com/cal.ics"))
        viewModel.handleIntent(ImportCalendarIntent.FileSelected("file:///sdcard/cal.ics"))
        assertEquals("", viewModel.uiState.value.url)
        assertEquals("file:///sdcard/cal.ics", viewModel.uiState.value.fileUri)

        viewModel.handleIntent(ImportCalendarIntent.UpdateUrl("https://example.com/cal.ics"))
        assertNull(viewModel.uiState.value.fileUri)
    }

    // endregion

    // region Import

    @Test
    fun `importing a selected file passes its content and options to the use case`() {
        val viewModel = viewModel()
        viewModel.handleIntent(ImportCalendarIntent.UpdateName("Feriados"))
        viewModel.handleIntent(ImportCalendarIntent.UpdateColor(42))
        viewModel.handleIntent(ImportCalendarIntent.ToggleAlarms(true))
        viewModel.handleIntent(ImportCalendarIntent.FileSelected(icsFile.toURI().toString()))

        viewModel.handleIntent(ImportCalendarIntent.SubmitImport)
        val state = viewModel.awaitIdle()

        assertTrue(state.isSuccess)
        assertNull(state.error)
        coVerify { importIcs(ICS, "Feriados", 42, true) }
    }

    @Test
    fun `importing from a url downloads its content`() {
        val viewModel = viewModel()
        viewModel.handleIntent(ImportCalendarIntent.UpdateName("Feriados"))
        viewModel.handleIntent(ImportCalendarIntent.UpdateUrl(icsFile.toURI().toString()))

        viewModel.handleIntent(ImportCalendarIntent.SubmitImport)

        assertTrue(viewModel.awaitIdle().isSuccess)
        coVerify { importIcs(ICS, "Feriados", any(), false) }
    }

    @Test
    fun `alarms are off by default for imported calendars`() {
        assertFalse(viewModel().uiState.value.alarmsEnabled)
    }

    @Test
    fun `each import failure is explained with a translated message`() {
        mapOf(
            ImportIcsException.NoEvents() to R.string.import_calendar_error_no_events,
            ImportIcsException.CalendarCreationFailed() to R.string.import_calendar_error_create_calendar,
            IllegalStateException("provider crashed") to R.string.import_calendar_error_generic,
        ).forEach { (failure, expected) ->
            coEvery { importIcs(any(), any(), any(), any()) } returns Result.failure(failure)
            val viewModel = viewModel()
            viewModel.handleIntent(ImportCalendarIntent.UpdateName("Feriados"))
            viewModel.handleIntent(ImportCalendarIntent.FileSelected(icsFile.toURI().toString()))

            viewModel.handleIntent(ImportCalendarIntent.SubmitImport)
            val state = viewModel.awaitIdle()

            assertFalse(state.isSuccess)
            assertEquals(message(expected), state.error)
        }
    }

    @Test
    fun `an unreachable source is reported instead of crashing`() {
        val viewModel = viewModel()
        viewModel.handleIntent(ImportCalendarIntent.UpdateName("Feriados"))
        viewModel.handleIntent(ImportCalendarIntent.UpdateUrl(File(tempFolder.root, "missing.ics").toURI().toString()))

        viewModel.handleIntent(ImportCalendarIntent.SubmitImport)
        val state = viewModel.awaitIdle()

        assertFalse(state.isSuccess)
        assertEquals(message(R.string.import_calendar_error_generic), state.error)
        coVerify(exactly = 0) { importIcs(any(), any(), any(), any()) }
    }

    @Test
    fun `dismissing the error and resetting success clear the flags`() {
        coEvery { importIcs(any(), any(), any(), any()) } returns Result.failure(Exception("boom"))
        val viewModel = viewModel()
        viewModel.handleIntent(ImportCalendarIntent.SubmitImport)

        viewModel.handleIntent(ImportCalendarIntent.DismissError)
        viewModel.handleIntent(ImportCalendarIntent.ResetSuccess)

        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    // endregion

    private fun viewModel() = ImportCalendarViewModel(importIcs, context)

    private fun message(resId: Int) = UiText.StringResource(resId)

    /** The import reads its source on Dispatchers.IO, so wait for the loading state to settle. */
    private fun ImportCalendarViewModel.awaitIdle(): ImportCalendarUiState =
        runBlocking { withTimeout(5_000) { uiState.first { !it.isLoading } } }

    private companion object {
        val ICS =
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:1
            DTSTART;VALUE=DATE:21001225
            SUMMARY:Natal
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()
    }
}
