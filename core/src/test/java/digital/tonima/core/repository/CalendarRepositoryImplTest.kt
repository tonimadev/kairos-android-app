package digital.tonima.core.repository

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.MatrixCursor
import android.provider.CalendarContract
import com.google.common.collect.ImmutableList
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
}
