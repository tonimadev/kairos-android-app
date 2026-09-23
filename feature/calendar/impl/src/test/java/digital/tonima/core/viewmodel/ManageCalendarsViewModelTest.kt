package digital.tonima.core.viewmodel

import digital.tonima.core.data.usecases.DeleteCalendarUseCase
import digital.tonima.core.data.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.data.usecases.UpdateCalendarUseCase
import digital.tonima.kairos.core.model.DeviceCalendar
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageCalendarsViewModelTest {
    private val getAvailableCalendars: GetAvailableCalendarsUseCase = mockk()
    private val updateCalendar: UpdateCalendarUseCase = mockk()
    private val deleteCalendar: DeleteCalendarUseCase = mockk()

    private val imported = DeviceCalendar(id = 10L, displayName = "Holidays", accountName = KAIROS_ACCOUNT, color = 1)
    private val google = DeviceCalendar(id = 1L, displayName = "me@gmail.com", accountName = "me@gmail.com")
    private val work = DeviceCalendar(id = 2L, displayName = "Work", accountName = "me@company.com")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { getAvailableCalendars() } returns listOf(google, imported, work)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `only calendars imported by Kairos are listed`() {
        val viewModel = viewModel()

        assertEquals(listOf(imported), viewModel.uiState.value.calendars)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `no imported calendars shows an empty list`() {
        coEvery { getAvailableCalendars() } returns listOf(google, work)

        assertTrue(viewModel().uiState.value.calendars.isEmpty())
    }

    @Test
    fun `opening the edit dialog prefills the calendar name and color`() {
        val viewModel = viewModel()

        viewModel.handleIntent(ManageCalendarsIntent.OpenEditDialog(imported))

        with(viewModel.uiState.value) {
            assertTrue(showEditDialog)
            assertEquals(imported.id, selectedCalendarId)
            assertEquals("Holidays", editName)
            assertEquals(1, editColor)
        }
    }

    @Test
    fun `saving an edit updates the calendar, closes the dialog and reloads`() {
        coEvery { updateCalendar(imported.id, "Feriados", 42) } returns true
        val viewModel = viewModel()
        viewModel.handleIntent(ManageCalendarsIntent.OpenEditDialog(imported))
        viewModel.handleIntent(ManageCalendarsIntent.UpdateEditName("Feriados"))
        viewModel.handleIntent(ManageCalendarsIntent.UpdateEditColor(42))

        viewModel.handleIntent(ManageCalendarsIntent.SaveEdit)

        coVerify { updateCalendar(imported.id, "Feriados", 42) }
        assertFalse(viewModel.uiState.value.showEditDialog)
        assertNull(viewModel.uiState.value.selectedCalendarId)
        coVerify(exactly = 2) { getAvailableCalendars() }
    }

    @Test
    fun `a failed edit keeps the dialog open`() {
        coEvery { updateCalendar(any(), any(), any()) } returns false
        val viewModel = viewModel()
        viewModel.handleIntent(ManageCalendarsIntent.OpenEditDialog(imported))

        viewModel.handleIntent(ManageCalendarsIntent.SaveEdit)

        assertTrue(viewModel.uiState.value.showEditDialog)
        coVerify(exactly = 1) { getAvailableCalendars() }
    }

    @Test
    fun `saving without a selected calendar does nothing`() {
        val viewModel = viewModel()

        viewModel.handleIntent(ManageCalendarsIntent.SaveEdit)

        coVerify(exactly = 0) { updateCalendar(any(), any(), any()) }
    }

    @Test
    fun `closing the dialog clears the selection`() {
        val viewModel = viewModel()
        viewModel.handleIntent(ManageCalendarsIntent.OpenEditDialog(imported))

        viewModel.handleIntent(ManageCalendarsIntent.CloseEditDialog)

        assertFalse(viewModel.uiState.value.showEditDialog)
        assertNull(viewModel.uiState.value.selectedCalendarId)
    }

    @Test
    fun `deleting a calendar reloads the list`() {
        coEvery { deleteCalendar(imported.id) } returns true
        val viewModel = viewModel()
        coEvery { getAvailableCalendars() } returns listOf(google, work)

        viewModel.handleIntent(ManageCalendarsIntent.DeleteCalendar(imported.id))

        coVerify { deleteCalendar(imported.id) }
        assertTrue(viewModel.uiState.value.calendars.isEmpty())
    }

    @Test
    fun `a failed delete keeps the current list`() {
        coEvery { deleteCalendar(any()) } returns false
        val viewModel = viewModel()

        viewModel.handleIntent(ManageCalendarsIntent.DeleteCalendar(imported.id))

        assertEquals(listOf(imported), viewModel.uiState.value.calendars)
        coVerify(exactly = 1) { getAvailableCalendars() }
    }

    private fun viewModel() = ManageCalendarsViewModel(getAvailableCalendars, updateCalendar, deleteCalendar)

    private companion object {
        const val KAIROS_ACCOUNT = "Kairos Imports"
    }
}
