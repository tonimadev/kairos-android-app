package digital.tonima.core.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import com.google.common.collect.ImmutableList
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.YearMonth
import java.time.ZoneId

/** Everything that writes to the user's calendar, plus recurrence and meeting-link detection. */
@RunWith(RobolectricTestRunner::class)
class CalendarRepositoryWritesTest {
    private val context = mockk<Context>()
    private val resolver = mockk<ContentResolver>()
    private lateinit var repository: CalendarRepositoryImpl

    @Before
    fun setUp() {
        every { context.contentResolver } returns resolver
        grantCalendar(read = true, write = true)
        repository = CalendarRepositoryImpl(context)
    }

    // region insertEvent

    @Test
    fun `timed events are saved in the device time zone and return the new id`() =
        runTest {
            val values = slot<ContentValues>()
            every { resolver.insert(CalendarContract.Events.CONTENT_URI, capture(values)) } returns
                Uri.parse("content://com.android.calendar/events/321")

            val id =
                repository.insertEvent(
                    7L,
                    "Dentist",
                    "Bring exams",
                    "Rua Augusta",
                    1_000L,
                    2_000L,
                    isAllDay = false,
                )

            assertEquals(321L, id)
            with(values.captured) {
                assertEquals(7L, getAsLong(CalendarContract.Events.CALENDAR_ID))
                assertEquals("Dentist", getAsString(CalendarContract.Events.TITLE))
                assertEquals("Bring exams", getAsString(CalendarContract.Events.DESCRIPTION))
                assertEquals("Rua Augusta", getAsString(CalendarContract.Events.EVENT_LOCATION))
                assertEquals(1_000L, getAsLong(CalendarContract.Events.DTSTART))
                assertEquals(2_000L, getAsLong(CalendarContract.Events.DTEND))
                assertEquals(ZoneId.systemDefault().id, getAsString(CalendarContract.Events.EVENT_TIMEZONE))
                assertEquals(0, getAsInteger(CalendarContract.Events.ALL_DAY))
                assertEquals(CalendarContract.Events.STATUS_CONFIRMED, getAsInteger(CalendarContract.Events.STATUS))
            }
        }

    @Test
    fun `all-day events are saved in UTC as the calendar provider requires`() =
        runTest {
            val values = slot<ContentValues>()
            every {
                resolver.insert(
                    any(),
                    capture(values),
                )
            } returns Uri.parse("content://com.android.calendar/events/1")

            repository.insertEvent(7L, "Holiday", null, null, 0L, 86_400_000L, isAllDay = true)

            assertEquals("UTC", values.captured.getAsString(CalendarContract.Events.EVENT_TIMEZONE))
            assertEquals(1, values.captured.getAsInteger(CalendarContract.Events.ALL_DAY))
            assertFalse(values.captured.containsKey(CalendarContract.Events.EVENT_LOCATION))
        }

    @Test
    fun `a failed insert returns no id`() =
        runTest {
            every { resolver.insert(any(), any()) } returns null

            assertNull(repository.insertEvent(7L, "x", null, null, 0L, 1L, isAllDay = false))
        }

    @Test
    fun `nothing is written without calendar write permission`() =
        runTest {
            grantCalendar(read = true, write = false)

            assertNull(repository.insertEvent(7L, "x", null, null, 0L, 1L, isAllDay = false))
            assertNull(repository.createLocalCalendar("Feriados", 0))
            assertFalse(repository.updateCalendar(10L, "Feriados", 0))
            assertFalse(repository.deleteCalendar(10L))
            verify(exactly = 0) { resolver.insert(any(), any()) }
            verify(exactly = 0) { resolver.update(any(), any(), any(), any()) }
            verify(exactly = 0) { resolver.delete(any(), any(), any()) }
        }

    // endregion

    // region rescheduleEvent

    @Test
    fun `rescheduling updates the start and end of that event only`() =
        runTest {
            val uri = slot<Uri>()
            val values = slot<ContentValues>()
            every { resolver.update(capture(uri), capture(values), null, null) } returns 1

            assertTrue(repository.rescheduleEvent(42L, 5_000L, 6_000L))

            assertEquals("content://com.android.calendar/events/42", uri.captured.toString())
            assertEquals(5_000L, values.captured.getAsLong(CalendarContract.Events.DTSTART))
            assertEquals(6_000L, values.captured.getAsLong(CalendarContract.Events.DTEND))
            assertEquals(2, values.captured.size())
        }

    @Test
    fun `rescheduling reports when the provider updated nothing or rejected the change`() =
        runTest {
            every { resolver.update(any(), any(), any(), any()) } returns 0
            assertFalse(repository.rescheduleEvent(42L, 5_000L, 6_000L))

            every { resolver.update(any(), any(), any(), any()) } throws IllegalArgumentException("all-day must be UTC")
            assertFalse(repository.rescheduleEvent(42L, 5_000L, 6_000L))
        }

    @Test
    fun `rescheduling needs calendar write permission`() =
        runTest {
            grantCalendar(read = true, write = false)

            assertFalse(repository.rescheduleEvent(42L, 5_000L, 6_000L))
            verify(exactly = 0) { resolver.update(any(), any(), any(), any()) }
        }

    // endregion

    // region Local calendars

    @Test
    fun `imported calendars are created in the local Kairos account`() =
        runTest {
            val uri = slot<Uri>()
            val values = slot<ContentValues>()
            every { resolver.insert(capture(uri), capture(values)) } returns
                Uri.parse("content://com.android.calendar/calendars/55")

            val id = repository.createLocalCalendar("Feriados", 0x123456)

            assertEquals(55L, id)
            assertSyncAdapterForKairos(uri.captured)
            with(values.captured) {
                assertEquals("Feriados", getAsString(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                assertEquals(0x123456, getAsInteger(CalendarContract.Calendars.CALENDAR_COLOR))
                assertEquals(KAIROS_ACCOUNT, getAsString(CalendarContract.Calendars.ACCOUNT_NAME))
                assertEquals(CalendarContract.ACCOUNT_TYPE_LOCAL, getAsString(CalendarContract.Calendars.ACCOUNT_TYPE))
                assertEquals(1, getAsInteger(CalendarContract.Calendars.VISIBLE))
            }
        }

    @Test
    fun `renaming only targets that calendar within the Kairos account`() =
        runTest {
            val uri = slot<Uri>()
            val values = slot<ContentValues>()
            every { resolver.update(capture(uri), capture(values), null, null) } returns 1

            assertTrue(repository.updateCalendar(10L, "Feriados BR", 42))

            assertEquals(10L, uri.captured.lastPathSegment?.toLong())
            assertSyncAdapterForKairos(uri.captured)
            assertEquals("Feriados BR", values.captured.getAsString(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
            assertEquals(42, values.captured.getAsInteger(CalendarContract.Calendars.CALENDAR_COLOR))
        }

    @Test
    fun `deleting only targets that calendar within the Kairos account`() =
        runTest {
            val uri = slot<Uri>()
            every { resolver.delete(capture(uri), null, null) } returns 1

            assertTrue(repository.deleteCalendar(10L))

            assertEquals(10L, uri.captured.lastPathSegment?.toLong())
            // The account parameters are what stop the provider from touching calendars of other
            // accounts (e.g. the user's Google calendar).
            assertSyncAdapterForKairos(uri.captured)
        }

    @Test
    fun `update and delete report when nothing matched`() =
        runTest {
            every { resolver.update(any(), any(), any(), any()) } returns 0
            every { resolver.delete(any(), any(), any()) } returns 0

            assertFalse(repository.updateCalendar(10L, "x", 0))
            assertFalse(repository.deleteCalendar(10L))
        }

    // endregion

    // region isRecurring

    @Test
    fun `events with a recurrence rule or dates are recurring`() =
        runTest {
            givenRecurrence(rrule = "FREQ=WEEKLY;BYDAY=MO", rdate = null)
            assertTrue(repository.isRecurring(1L))

            givenRecurrence(rrule = null, rdate = "20260101T090000Z")
            assertTrue(repository.isRecurring(1L))

            givenRecurrence(rrule = " ", rdate = null)
            assertFalse(repository.isRecurring(1L))
        }

    @Test
    fun `unknown events and missing permission are not recurring`() =
        runTest {
            every { resolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any()) } returns
                MatrixCursor(arrayOf(CalendarContract.Events.RRULE, CalendarContract.Events.RDATE))
            assertFalse(repository.isRecurring(1L))

            grantCalendar(read = false, write = false)
            assertFalse(repository.isRecurring(1L))
        }

    // endregion

    // region Meeting links

    @Test
    fun `google meet links are detected`() = assertMeetingLink("https://meet.google.com/abc-defg-hij")

    @Test
    fun `teams links are detected`() = assertMeetingLink("https://teams.microsoft.com/l/meetup-join/19%3ameeting")

    @Test
    fun `webex links are detected`() = assertMeetingLink("https://acme.webex.com/meet/bob")

    @Test
    fun `google meet link in the location field is found`() =
        runTest {
            assertEquals(
                "https://meet.google.com/abc-defg-hij",
                eventWith(description = "Weekly sync", location = "https://meet.google.com/abc-defg-hij").meetingUrl,
            )
        }

    @Test
    fun `zoom links are detected`() = assertMeetingLink("https://us02web.zoom.us/j/123456789?pwd=x")

    @Test
    fun `jitsi links are detected`() = assertMeetingLink("https://meet.jit.si/KairosRoom")

    @Test
    fun `skype links are detected`() = assertMeetingLink("https://join.skype.com/AbCdEf123")

    private fun assertMeetingLink(link: String) =
        runTest {
            assertEquals(link, eventWith(description = "Entrar: $link agora").meetingUrl)
        }

    @Test
    fun `meeting link in the location field is found too`() =
        runTest {
            assertEquals(
                "https://us02web.zoom.us/j/123456789",
                eventWith(description = null, location = "https://us02web.zoom.us/j/123456789").meetingUrl,
            )
        }

    @Test
    fun `events without a meeting link have none`() =
        runTest {
            assertNull(eventWith(description = "Lunch at https://example.com/menu").meetingUrl)
        }

    // endregion

    private suspend fun eventWith(
        description: String?,
        location: String? = null,
    ) = run {
        val cursor =
            MatrixCursor(
                arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Instances.CALENDAR_COLOR,
                    CalendarContract.Instances.DESCRIPTION,
                    CalendarContract.Instances.EVENT_LOCATION,
                    CalendarContract.Events.RRULE,
                    CalendarContract.Events.RDATE,
                    CalendarContract.Instances.AVAILABILITY,
                    "event_type",
                ),
            )
        cursor.addRow(
            arrayOf<Any?>(
                1L,
                "Meeting",
                1_715_760_000_000L,
                1_715_763_600_000L,
                0,
                1L,
                0,
                description,
                location,
                null,
                null,
                0,
                0,
            ),
        )
        every { resolver.query(any(), any(), any(), any(), any()) } returns cursor
        givenRecurrence(rrule = null, rdate = null)
        repository.getEventsForMonth(YearMonth.of(2024, 5).atDay(1).toEpochDay(), ImmutableList.of(1L)).single()
    }

    private fun givenRecurrence(
        rrule: String?,
        rdate: String?,
    ) {
        val cursor = MatrixCursor(arrayOf(CalendarContract.Events.RRULE, CalendarContract.Events.RDATE))
        cursor.addRow(arrayOf(rrule, rdate))
        every { resolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any()) } returns cursor
    }

    private fun assertSyncAdapterForKairos(uri: Uri) {
        assertEquals("true", uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER))
        assertEquals(KAIROS_ACCOUNT, uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME))
        assertEquals(
            CalendarContract.ACCOUNT_TYPE_LOCAL,
            uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE),
        )
    }

    private fun grantCalendar(
        read: Boolean,
        write: Boolean,
    ) {
        val readResult = if (read) PERMISSION_GRANTED else PERMISSION_DENIED
        val writeResult = if (write) PERMISSION_GRANTED else PERMISSION_DENIED
        every { context.checkPermission(READ, any(), any()) } returns readResult
        every { context.checkPermission(WRITE, any(), any()) } returns writeResult
        every { context.checkSelfPermission(READ) } returns readResult
        every { context.checkSelfPermission(WRITE) } returns writeResult
    }

    private companion object {
        const val KAIROS_ACCOUNT = "Kairos Imports"
        const val READ = "android.permission.READ_CALENDAR"
        const val WRITE = "android.permission.WRITE_CALENDAR"
    }
}
