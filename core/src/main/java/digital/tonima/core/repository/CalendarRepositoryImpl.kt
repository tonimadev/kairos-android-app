package digital.tonima.core.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.logcat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = CalendarRepository::class)
class CalendarRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) :
    CalendarRepository {
        private val eventProjection: Array<String> =
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.CALENDAR_COLOR,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_LOCATION,
            )

        private val projectionIdIndex = 0
        private val projectionTitleIndex = 1
        private val projectionBeginIndex = 2
        private val projectionAllDayIndex = 3
        private val projectionCalendarColorIndex = 5
        private val projectionDescriptionIndex = 6
        private val projectionLocationIndex = 7

        private fun hasCalendarPermission() =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

        private fun hasWriteCalendarPermission() =
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

        override suspend fun getAvailableCalendars(): List<DeviceCalendar> =
            withContext(Dispatchers.IO) {
                if (!hasCalendarPermission()) {
                    logcat { "Tentativa de aceder aos calendários sem a permissão READ_CALENDAR." }
                    return@withContext emptyList()
                }

                val calendars = mutableListOf<DeviceCalendar>()
                val projection =
                    arrayOf(
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                        CalendarContract.Calendars.ACCOUNT_NAME,
                        CalendarContract.Calendars.CALENDAR_COLOR,
                        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    )

                val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
                val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

                val cursor =
                    context.contentResolver.query(
                        CalendarContract.Calendars.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                    )

                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getLong(0)
                        val displayName = it.getString(1) ?: ""
                        val accountName = it.getString(2) ?: ""
                        val color = it.getInt(3)
                        calendars.add(DeviceCalendar(id, displayName, accountName, color))
                    }
                }
                return@withContext calendars
            }

        override suspend fun getEventsForMonth(
            yearMonth: YearMonth,
            allowedCalendarIds: List<Long>,
        ): List<Event> =
            withContext(Dispatchers.IO) {
                if (!hasCalendarPermission()) {
                    logcat { "Tentativa de aceder ao calendário sem a permissão READ_CALENDAR." }
                    return@withContext emptyList()
                }
                val events = mutableListOf<Event>()

                val startMillis =
                    yearMonth.atDay(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                val endMillis =
                    yearMonth.atEndOfMonth()
                        .atTime(23, 59, 59)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(builder, startMillis)
                ContentUris.appendId(builder, endMillis)
                val uri = builder.build()

                val selection: String?
                val selectionArgs: Array<String>?
                if (allowedCalendarIds.isNotEmpty()) {
                    val questionMarks = allowedCalendarIds.joinToString(",") { "?" }
                    selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($questionMarks)"
                    selectionArgs = allowedCalendarIds.map { it.toString() }.toTypedArray()
                } else {
                    selection = null
                    selectionArgs = null
                }

                val cursor =
                    context.contentResolver.query(
                        uri,
                        eventProjection,
                        selection,
                        selectionArgs,
                        null,
                    )

                cursor?.use {
                    while (it.moveToNext()) {
                        val eventId = it.getLong(projectionIdIndex)
                        val title = it.getString(projectionTitleIndex)
                        val begin = it.getLong(projectionBeginIndex)
                        val isAllDay = it.getInt(projectionAllDayIndex) == 1
                        val color = it.getInt(projectionCalendarColorIndex)
                        val description = it.getString(projectionDescriptionIndex)
                        val location = it.getString(projectionLocationIndex)

                        events.add(
                            Event(
                                id = eventId,
                                title = title,
                                startTime = begin,
                                isAllDay = isAllDay,
                                calendarColor = color,
                                meetingUrl = extractMeetLink(description, location),
                                location = location,
                            ),
                        )
                    }
                }

                val enriched =
                    events
                        .sortedBy { it.startTime }
                        .map { e ->
                            val recurring =
                                try {
                                    isRecurring(e.id)
                                } catch (t: Throwable) {
                                    false
                                }
                            e.copy(isRecurring = recurring)
                        }
                return@withContext enriched
            }

        override suspend fun getNextUpcomingEvent(allowedCalendarIds: List<Long>): Event? =
            withContext(Dispatchers.IO) {
                if (!hasCalendarPermission()) {
                    logcat { "Tentativa de aceder ao calendário sem a permissão READ_CALENDAR." }
                    return@withContext null
                }

                val now = Instant.now()
                val startMillis = now.toEpochMilli()
                val endMillis = now.plus(30, ChronoUnit.DAYS).toEpochMilli()

                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(builder, startMillis)
                ContentUris.appendId(builder, endMillis)
                val uri = builder.build()

                val baseCondition = "${CalendarContract.Instances.END} > ?"
                val selection: String
                val selectionArgs: Array<String>
                if (allowedCalendarIds.isNotEmpty()) {
                    val questionMarks = allowedCalendarIds.joinToString(",") { "?" }
                    selection =
                        "$baseCondition AND ${CalendarContract.Instances.CALENDAR_ID} IN ($questionMarks)"
                    selectionArgs = arrayOf(now.toEpochMilli().toString()) +
                        allowedCalendarIds.map { id -> id.toString() }.toTypedArray()
                } else {
                    selection = baseCondition
                    selectionArgs = arrayOf(now.toEpochMilli().toString())
                }

                val cursor =
                    context.contentResolver.query(
                        uri,
                        eventProjection,
                        selection,
                        selectionArgs,
                        null,
                    )

                var nextEvent: Event? = null
                cursor?.use {
                    if (it.moveToFirst()) {
                        val eventId = it.getLong(projectionIdIndex)
                        val title = it.getString(projectionTitleIndex)
                        val begin = it.getLong(projectionBeginIndex)
                        val isAllDay = it.getInt(projectionAllDayIndex) == 1
                        val color = it.getInt(projectionCalendarColorIndex)
                        val description = it.getString(projectionDescriptionIndex)
                        val location = it.getString(projectionLocationIndex)
                        nextEvent =
                            Event(
                                id = eventId,
                                title = title,
                                startTime = begin,
                                isAllDay = isAllDay,
                                calendarColor = color,
                                meetingUrl = extractMeetLink(description, location),
                                location = location,
                            )
                    }
                }
                return@withContext nextEvent
            }

        override suspend fun isRecurring(eventId: Long): Boolean =
            withContext(Dispatchers.IO) {
                if (!hasCalendarPermission()) {
                    logcat { "Tentativa de aceder ao calendário sem a permissão READ_CALENDAR." }
                    return@withContext false
                }

                val projection =
                    arrayOf(
                        CalendarContract.Events.RRULE,
                        CalendarContract.Events.RDATE,
                    )
                val selection = "${CalendarContract.Events._ID} = ?"
                val selectionArgs = arrayOf(eventId.toString())

                val cursor =
                    context.contentResolver.query(
                        CalendarContract.Events.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                    )

                var recurring = false
                cursor?.use {
                    if (it.moveToFirst()) {
                        val rrule = it.getString(0)
                        val rdate = it.getString(1)
                        recurring = !rrule.isNullOrBlank() || !rdate.isNullOrBlank()
                    }
                }
                return@withContext recurring
            }

        override suspend fun insertEvent(
            calendarId: Long,
            title: String,
            description: String?,
            location: String?,
            startTime: Long,
            endTime: Long,
            isAllDay: Boolean,
        ): Long? =
            withContext(Dispatchers.IO) {
                if (!hasWriteCalendarPermission()) {
                    logcat { "Tentativa de inserir evento sem a permissão WRITE_CALENDAR." }
                    return@withContext null
                }

                val values =
                    android.content.ContentValues().apply {
                        if (isAllDay) {
                            put(CalendarContract.Events.DTSTART, startTime)
                            put(CalendarContract.Events.DTEND, endTime)
                            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                        } else {
                            put(CalendarContract.Events.DTSTART, startTime)
                            put(CalendarContract.Events.DTEND, endTime)
                            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
                        }
                        put(CalendarContract.Events.TITLE, title)
                        put(CalendarContract.Events.DESCRIPTION, description)
                        put(CalendarContract.Events.CALENDAR_ID, calendarId)
                        put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
                        put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                        if (location != null) {
                            put(CalendarContract.Events.EVENT_LOCATION, location)
                        }
                    }

                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                return@withContext uri?.lastPathSegment?.toLongOrNull()
            }

        private fun extractMeetLink(
            description: String?,
            location: String?,
        ): String? {
            if (description == null && location == null) return null
            val combinedText = "${description ?: ""} ${location ?: ""}"
            val meetRegex = "https://meet\\.google\\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}".toRegex()
            return meetRegex.find(combinedText)?.value
        }
    }
