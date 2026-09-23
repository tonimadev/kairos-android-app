package digital.tonima.core.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.MatrixCursor
import android.provider.CalendarContract
import com.google.common.collect.ImmutableList
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class CalendarRepositoryImplTest {
    private val mockContext = mockk<Context>()
    private val mockContentResolver = mockk<ContentResolver>()
    private lateinit var repository: CalendarRepositoryImpl

    @Before
    fun setup() {
        every { mockContext.contentResolver } returns mockContentResolver
        // Mock permission check
        every {
            mockContext.checkPermission(
                "android.permission.READ_CALENDAR",
                any(),
                any(),
            )
        } returns PERMISSION_GRANTED

        // ContextCompat.checkSelfPermission calls context.checkPermission
        // But it's a static call. MockK can mock static calls if needed,
        // but here we are lucky that CalendarRepositoryImpl uses ContextCompat.
        // Wait, ContextCompat.checkSelfPermission(context, ...) calls context.checkSelfPermission(...)
        // which is available on API 23+.
        every { mockContext.checkSelfPermission("android.permission.READ_CALENDAR") } returns PERMISSION_GRANTED
        every { mockContext.checkSelfPermission("android.permission.WRITE_CALENDAR") } returns PERMISSION_GRANTED

        repository = CalendarRepositoryImpl(mockContext)
    }

    @Test
    fun `when getAvailableCalendars is called then returns list of calendars`() =
        runTest {
            val cursor =
                MatrixCursor(
                    arrayOf(
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                        CalendarContract.Calendars.ACCOUNT_NAME,
                        CalendarContract.Calendars.CALENDAR_COLOR,
                        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    ),
                )
            cursor.addRow(arrayOf<Any?>(1L, "Work", "user@example.com", 0xFF0000, 700))

            every {
                mockContentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns cursor

            val calendars = repository.getAvailableCalendars()

            assertEquals(1, calendars.size)
            assertEquals("Work", calendars[0].displayName)
            assertEquals(1L, calendars[0].id)
        }

    @Test
    fun `when getEventsForMonth is called then returns events from cursor`() =
        runTest {
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
                    101L,
                    "Meeting",
                    1715760000000L,
                    1715763600000L,
                    0,
                    1L,
                    0xFF0000,
                    "Desc",
                    "Loc",
                    null,
                    null,
                    0, // AVAILABILITY_BUSY
                    0, // Regular event
                ),
            )

            every {
                mockContentResolver.query(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns cursor

            // Mock isRecurring call which also does a query
            val recurringCursor = MatrixCursor(arrayOf(CalendarContract.Events.RRULE, CalendarContract.Events.RDATE))
            recurringCursor.addRow(arrayOf(null, null))
            every {
                mockContentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns recurringCursor

            val events =
                repository.getEventsForMonth(
                    YearMonth.of(2024, 5).atDay(1).toEpochDay(),
                    ImmutableList.of(1L),
                )

            assertEquals(1, events.size)
            assertEquals("Meeting", events[0].title)
            assertEquals(101L, events[0].id)
        }

    @Test
    fun `when getEventsForMonth finds free or work location events then they are filtered out`() =
        runTest {
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
            // Event 1: Busy (Normal)
            cursor.addRow(
                arrayOf<Any?>(
                    101L,
                    "Meeting",
                    1715760000000L,
                    1715763600000L,
                    0,
                    1L,
                    0xFF0000,
                    "Desc",
                    "Loc",
                    null,
                    null,
                    0,
                    0,
                ),
            )
            // Event 2: Free
            cursor.addRow(
                arrayOf<Any?>(
                    102L,
                    "Free Event",
                    1715760000000L,
                    1715763600000L,
                    0,
                    1L,
                    0xFF0000,
                    "Desc",
                    "Loc",
                    null,
                    null,
                    1,
                    0,
                ),
            )
            // Event 3: Work Location (TYPE_WORK_LOCATION = 2)
            cursor.addRow(
                arrayOf<Any?>(
                    103L,
                    "Work Location",
                    1715760000000L,
                    1715763600000L,
                    0,
                    1L,
                    0xFF0000,
                    "Desc",
                    "Loc",
                    null,
                    null,
                    0,
                    2,
                ),
            )

            every {
                mockContentResolver.query(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns cursor

            val events =
                repository.getEventsForMonth(
                    YearMonth.of(2024, 5).atDay(1).toEpochDay(),
                    ImmutableList.of(1L),
                )

            assertEquals(1, events.size)
            assertEquals("Meeting", events[0].title)
        }

    @Test
    @Config(sdk = [34])
    fun `when CalendarProvider rejects the event_type column then getEventsForMonth falls back`() =
        runTest {
            // Some OEM/ROM CalendarProvider implementations reject the "event_type" projection
            // column with IllegalArgumentException even on API 34+ (SDK_INT alone isn't a
            // reliable signal of column support). The repository must retry without it instead
            // of letting the exception propagate and crash the caller.
            val fallbackCursor =
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
                    ),
                )
            fallbackCursor.addRow(
                arrayOf<Any?>(
                    101L,
                    "Meeting",
                    1715760000000L,
                    1715763600000L,
                    0,
                    1L,
                    0xFF0000,
                    "Desc",
                    "Loc",
                    null,
                    null,
                    0,
                ),
            )

            every {
                mockContentResolver.query(
                    any(),
                    match<Array<String>> { projection -> projection.contains("event_type") },
                    any(),
                    any(),
                    any(),
                )
            } throws IllegalArgumentException("Invalid column event_type")

            every {
                mockContentResolver.query(
                    any(),
                    match<Array<String>> { projection -> !projection.contains("event_type") },
                    any(),
                    any(),
                    any(),
                )
            } returns fallbackCursor

            val events =
                repository.getEventsForMonth(
                    YearMonth.of(2024, 5).atDay(1).toEpochDay(),
                    ImmutableList.of(1L),
                )

            assertEquals(1, events.size)
            assertEquals("Meeting", events[0].title)
        }

    @Test
    fun `when getNextUpcomingEvent finds free event then it skips to next busy event`() =
        runTest {
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
            // Event 1: Free
            cursor.addRow(
                arrayOf<Any?>(
                    101L,
                    "Free Event",
                    1715760000000L,
                    1715763600000L,
                    0,
                    1L,
                    0xFF0000,
                    "Desc",
                    "Loc",
                    null,
                    null,
                    1,
                    0,
                ),
            )
            // Event 2: Busy
            cursor.addRow(
                arrayOf<Any?>(
                    102L,
                    "Busy Meeting",
                    1715770000000L,
                    1715773600000L,
                    0,
                    1L,
                    0xFF0000,
                    "Desc",
                    "Loc",
                    null,
                    null,
                    0,
                    0,
                ),
            )

            every {
                mockContentResolver.query(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns cursor

            val nextEvent = repository.getNextUpcomingEvent(ImmutableList.of(1L))

            assertEquals("Busy Meeting", nextEvent?.title)
            assertEquals(102L, nextEvent?.id)
        }

    @Test
    fun `without calendar permission nothing is read`() =
        runTest {
            every { mockContext.checkSelfPermission("android.permission.READ_CALENDAR") } returns PERMISSION_DENIED
            every { mockContext.checkPermission("android.permission.READ_CALENDAR", any(), any()) } returns
                PERMISSION_DENIED

            assertTrue(repository.getAvailableCalendars().isEmpty())
            val thisMonth = YearMonth.now().atDay(1).toEpochDay()
            assertTrue(repository.getEventsForMonth(thisMonth, ImmutableList.of()).isEmpty())
            assertNull(repository.getNextUpcomingEvent(ImmutableList.of()))
            verify(exactly = 0) { mockContentResolver.query(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `with no calendar filter every calendar is queried`() =
        runTest {
            every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns null

            val events = repository.getEventsForMonth(YearMonth.now().atDay(1).toEpochDay(), ImmutableList.of())

            assertTrue(events.isEmpty())
            verify { mockContentResolver.query(any(), any(), isNull(), isNull(), any()) }
        }

    @Test
    fun `the next event skips birthdays and reads recurrence and all-day flags`() =
        runTest {
            val cursor = upcomingCursor()
            cursor.addRow(arrayOf<Any?>(201L, "Aniversário", 1L, 2L, 1, 1L, 0, null, null, null, null, 0, 3))
            cursor.addRow(
                arrayOf<Any?>(202L, "Feriado", 3L, 4L, 1, 1L, 0, null, null, null, "20240101", 0, 0),
            )

            val selection = slot<String>()
            every { mockContentResolver.query(any(), any(), capture(selection), any(), any()) } returns cursor

            val next = repository.getNextUpcomingEvent(ImmutableList.of())

            assertEquals(202L, next?.id)
            assertTrue(next!!.isAllDay)
            assertTrue(next.isRecurring)
            assertEquals("${CalendarContract.Instances.END} > ?", selection.captured)
        }

    @Test
    fun `no upcoming event when the calendar is empty`() =
        runTest {
            every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns upcomingCursor()

            assertNull(repository.getNextUpcomingEvent(ImmutableList.of(1L)))
        }

    @Test
    fun `calendars without a name or account get empty strings`() =
        runTest {
            val cursor =
                MatrixCursor(
                    arrayOf(
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                        CalendarContract.Calendars.ACCOUNT_NAME,
                        CalendarContract.Calendars.CALENDAR_COLOR,
                    ),
                )
            cursor.addRow(arrayOf<Any?>(1L, null, null, 0))
            every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns cursor

            val calendar = repository.getAvailableCalendars().single()

            assertEquals("", calendar.displayName)
            assertEquals("", calendar.accountName)
            assertEquals(1L, calendar.id)
        }

    @Test
    fun `declined invitations are left out of the month and every other response is kept`() =
        runTest {
            val cursor = attendeeStatusCursor()
            cursor.addRow(instanceRow(301L, "Recusada", CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED))
            cursor.addRow(instanceRow(302L, "Aceita", CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED))
            cursor.addRow(instanceRow(303L, "Sem resposta", CalendarContract.Attendees.ATTENDEE_STATUS_INVITED))
            cursor.addRow(instanceRow(304L, "Talvez", CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE))
            cursor.addRow(instanceRow(305L, "Sem convidados", CalendarContract.Attendees.ATTENDEE_STATUS_NONE))
            every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns cursor

            val events =
                repository.getEventsForMonth(
                    YearMonth.of(2024, 5).atDay(1).toEpochDay(),
                    ImmutableList.of(1L),
                )

            assertEquals(listOf(302L, 303L, 304L, 305L), events.map { it.id })
        }

    @Test
    fun `the next event skips a declined invitation`() =
        runTest {
            val cursor = attendeeStatusCursor()
            cursor.addRow(instanceRow(401L, "Recusada", CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED))
            cursor.addRow(instanceRow(402L, "Aceita", CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED))
            every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns cursor

            val next = repository.getNextUpcomingEvent(ImmutableList.of(1L))

            assertEquals(402L, next?.id)
        }

    @Test
    fun `the attendee status is requested from the provider`() =
        runTest {
            val projection = slot<Array<String>>()
            every { mockContentResolver.query(any(), capture(projection), any(), any(), any()) } returns null

            repository.getEventsForMonth(YearMonth.of(2024, 5).atDay(1).toEpochDay(), ImmutableList.of(1L))

            assertTrue(projection.captured.contains(CalendarContract.Instances.SELF_ATTENDEE_STATUS))
        }

    private fun attendeeStatusCursor() =
        MatrixCursor(upcomingCursor().columnNames + CalendarContract.Instances.SELF_ATTENDEE_STATUS)

    private fun instanceRow(
        id: Long,
        title: String,
        attendeeStatus: Int,
    ) = arrayOf<Any?>(
        id,
        title,
        1715760000000L + id,
        1715763600000L + id,
        0,
        1L,
        0,
        null,
        null,
        null,
        null,
        0,
        0,
        attendeeStatus,
    )

    private fun upcomingCursor() =
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
}
