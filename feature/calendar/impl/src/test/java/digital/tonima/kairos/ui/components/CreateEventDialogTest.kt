package digital.tonima.kairos.ui.components

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.viewmodel.VoiceEventData
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.DeviceCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CreateEventDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val personal = DeviceCalendar(id = 1L, displayName = "Personal", accountName = "me@gmail.com")
    private val work = DeviceCalendar(id = 2L, displayName = "Work", accountName = "me@company.com")

    private data class Created(
        val calendarId: Long,
        val title: String,
        val description: String?,
        val location: String?,
        val start: Long,
        val end: Long,
        val isAllDay: Boolean,
    )

    private val created = mutableListOf<Created>()
    private var dismissed = 0

    @Test
    fun `create is disabled until the event has a title`() {
        render()

        createButton().assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.event_title)).performTextInput("Dentist")
        createButton().assertIsEnabled()
    }

    @Test
    fun `create is disabled when there is no calendar to save into`() {
        render(calendars = emptyList(), voice = VoiceEventData(title = "Dentist"))

        createButton().assertIsNotEnabled()
    }

    @Test
    fun `voice data prefills the event and is created as dictated`() {
        val start = LocalDateTime.of(2100, 3, 10, 14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = start + 45 * 60_000L
        render(
            voice =
                VoiceEventData(
                    title = "Dentist",
                    description = "Bring exams",
                    location = "Rua Augusta, 500",
                    startTime = start,
                    endTime = end,
                ),
        )

        createButton().performClick()

        assertEquals(
            listOf(Created(personal.id, "Dentist", "Bring exams", "Rua Augusta, 500", start, end, false)),
            created,
        )
    }

    @Test
    fun `new events default to one hour on the selected day in the first calendar`() {
        val day = LocalDate.of(2100, 3, 10)
        render(initialDay = day)

        compose.onNodeWithText(string(R.string.event_title)).performTextInput("Planning")
        createButton().performClick()

        val event = created.single()
        assertEquals(personal.id, event.calendarId)
        assertEquals(
            day,
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(event.start), ZoneId.systemDefault()).toLocalDate(),
        )
        assertEquals(60 * 60_000L, event.end - event.start)
        assertNull("Blank description is saved as null", event.description)
        assertNull("Blank location is saved as null", event.location)
    }

    @Test
    fun `all-day events span whole UTC days and hide the time fields`() {
        val day = LocalDate.of(2100, 3, 10)
        render(initialDay = day, voice = VoiceEventData(title = "Holiday"))

        compose.onNode(isToggleable()).performClick()
        compose.onAllNodesWithText(string(R.string.start_time)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.end_time)).assertCountEquals(0)
        createButton().performClick()

        val event = created.single()
        assertTrue(event.isAllDay)
        assertEquals(day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), event.start)
        assertEquals(day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), event.end)
    }

    @Test
    fun `the calendar can be changed before creating`() {
        render(voice = VoiceEventData(title = "Review"))

        compose.onNodeWithContentDescription(string(R.string.cd_expand_settings)).performClick()
        compose.onNodeWithText("Work").performClick()
        createButton().performClick()

        assertEquals(work.id, created.single().calendarId)
    }

    @Test
    fun `confirming the pickers without changes keeps the chosen times`() {
        val start = LocalDateTime.of(2100, 3, 10, 9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        render(voice = VoiceEventData(title = "Standup", startTime = start, endTime = start + 15 * 60_000L))

        listOf(R.string.start_date, R.string.start_time, R.string.end_date, R.string.end_time).forEach { field ->
            compose.onNodeWithText(string(field)).performScrollTo().performClick()
            compose.onNodeWithText("OK").performClick()
        }
        createButton().performClick()

        assertEquals(start, created.single().start)
        assertEquals(start + 15 * 60_000L, created.single().end)
    }

    @Test
    fun `cancel dismisses without creating`() {
        render(voice = VoiceEventData(title = "Dentist"))

        compose.onNodeWithText(string(R.string.cancel)).performClick()

        assertEquals(1, dismissed)
        assertTrue(created.isEmpty())
    }

    private fun render(
        calendars: List<DeviceCalendar> = listOf(personal, work),
        initialDay: LocalDate = LocalDate.of(2100, 3, 10),
        voice: VoiceEventData? = null,
    ) {
        compose.setContent {
            CreateEventDialog(
                onDismiss = { dismissed++ },
                onCreate = { calendarId, title, description, location, start, end, isAllDay ->
                    created += Created(calendarId, title, description, location, start, end, isAllDay)
                },
                availableCalendars = calendars,
                initialDateEpochDays = initialDay.toEpochDay(),
                voiceEventData = voice,
            )
        }
    }

    private fun createButton() = compose.onNodeWithText(string(R.string.create))

    private fun string(resId: Int) = app.getString(resId)
}
