package digital.tonima.core.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.google.common.collect.ImmutableList
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.logcat
import java.time.Instant
import java.time.LocalDate
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
        private fun getEventProjection(): Array<String> {
            val projection =
                mutableListOf(
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
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                projection.add("event_type")
            }
            return projection.toTypedArray()
        }

        private fun hasCalendarPermission() =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

        private fun hasWriteCalendarPermission() =
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

        override suspend fun getAvailableCalendars(): ImmutableList<DeviceCalendar> =
            withContext(Dispatchers.IO) {
                if (!hasCalendarPermission()) {
                    logcat { "Tentativa de aceder aos calendários sem a permissão READ_CALENDAR." }
                    return@withContext ImmutableList.of()
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
                return@withContext ImmutableList.copyOf(calendars)
            }

        override suspend fun getEventsForMonth(
            yearMonth: Long,
            allowedCalendarIds: ImmutableList<Long>,
        ): ImmutableList<Event> =
            withContext(Dispatchers.IO) {
                if (!hasCalendarPermission()) {
                    logcat { "Tentativa de aceder ao calendário sem a permissão READ_CALENDAR." }
                    return@withContext ImmutableList.of()
                }
                val events = mutableListOf<Event>()

                val parsedYearMonth = YearMonth.from(LocalDate.ofEpochDay(yearMonth))

                val startMillis =
                    parsedYearMonth.atDay(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                val endMillis =
                    parsedYearMonth.atEndOfMonth()
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

                val projection = getEventProjection()
                val cursor =
                    context.contentResolver.query(
                        uri,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                    )

                cursor?.use {
                    val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                    val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                    val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                    val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                    val colorIdx = it.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR)
                    val descIdx = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                    val locIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                    val rruleIdx = it.getColumnIndex(CalendarContract.Events.RRULE)
                    val rdateIdx = it.getColumnIndex(CalendarContract.Events.RDATE)
                    val availIdx = it.getColumnIndex(CalendarContract.Instances.AVAILABILITY)
                    val typeIdx =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            it.getColumnIndex("event_type")
                        } else {
                            -1
                        }

                    while (it.moveToNext()) {
                        val eventId = it.getLong(idIdx)
                        val title = it.getString(titleIdx)
                        val begin = it.getLong(beginIdx)
                        val end = it.getLong(endIdx)
                        val isAllDay = it.getInt(allDayIdx) == 1
                        val color = it.getInt(colorIdx)
                        val description = it.getString(descIdx)
                        val location = it.getString(locIdx)
                        val rrule = it.getString(rruleIdx)
                        val rdate = it.getString(rdateIdx)
                        val availability = it.getInt(availIdx)
                        val eventType = if (typeIdx != -1) it.getInt(typeIdx) else 0

                        val isRecurring = !rrule.isNullOrBlank() || !rdate.isNullOrBlank()

                        // Filter out "Free" events and special Google Calendar types (Work Location, Focus Time, etc.)
                        if (availability == CalendarContract.Instances.AVAILABILITY_FREE) {
                            continue
                        }

                        // event_type values (API 34+): 2 = TYPE_WORK_LOCATION, 3 = TYPE_FOCUS_TIME
                        if (typeIdx != -1 && (eventType == 2 || eventType == 3)) {
                            continue
                        }

                        events.add(
                            Event(
                                id = eventId,
                                title = title,
                                startTime = begin,
                                endTime = end,
                                isAllDay = isAllDay,
                                isRecurring = isRecurring,
                                calendarColor = color,
                                meetingUrl = extractMeetLink(description, location),
                                location = location,
                                availability = availability,
                                eventType = eventType,
                            ),
                        )
                    }
                }

                return@withContext ImmutableList.copyOf(events.sortedBy { it.startTime })
            }

        override suspend fun getNextUpcomingEvent(allowedCalendarIds: ImmutableList<Long>): Event? =
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

                val projection = getEventProjection()
                val cursor =
                    context.contentResolver.query(
                        uri,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                    )

                var nextEvent: Event? = null
                cursor?.use {
                    val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                    val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                    val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                    val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                    val colorIdx = it.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR)
                    val descIdx = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                    val locIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                    val rruleIdx = it.getColumnIndex(CalendarContract.Events.RRULE)
                    val rdateIdx = it.getColumnIndex(CalendarContract.Events.RDATE)
                    val availIdx = it.getColumnIndex(CalendarContract.Instances.AVAILABILITY)
                    val typeIdx =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            it.getColumnIndex("event_type")
                        } else {
                            -1
                        }

                    if (it.moveToFirst()) {
                        do {
                            val eventId = it.getLong(idIdx)
                            val title = it.getString(titleIdx)
                            val begin = it.getLong(beginIdx)
                            val end = it.getLong(endIdx)
                            val isAllDay = it.getInt(allDayIdx) == 1
                            val color = it.getInt(colorIdx)
                            val description = it.getString(descIdx)
                            val location = it.getString(locIdx)
                            val rrule = it.getString(rruleIdx)
                            val rdate = it.getString(rdateIdx)
                            val availability = it.getInt(availIdx)
                            val eventType = if (typeIdx != -1) it.getInt(typeIdx) else 0

                            val isRecurring = !rrule.isNullOrBlank() || !rdate.isNullOrBlank()

                            // Filter out "Free" events and special Google
                            // Calendar types (Work Location, Focus Time, etc.)
                            if (availability == CalendarContract.Instances.AVAILABILITY_FREE) {
                                continue
                            }
                            if (typeIdx != -1 && (eventType == 2 || eventType == 3)) {
                                continue
                            }

                            nextEvent =
                                Event(
                                    id = eventId,
                                    title = title,
                                    startTime = begin,
                                    endTime = end,
                                    isAllDay = isAllDay,
                                    isRecurring = isRecurring,
                                    calendarColor = color,
                                    meetingUrl = extractMeetLink(description, location),
                                    location = location,
                                    availability = availability,
                                    eventType = eventType,
                                )
                            break
                        } while (it.moveToNext())
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

        private fun asSyncAdapter(
            uri: Uri,
            accountName: String,
            accountType: String,
        ): Uri {
            return uri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                .build()
        }

        override suspend fun createLocalCalendar(
            name: String,
            color: Int,
        ): Long? =
            withContext(Dispatchers.IO) {
                if (!hasWriteCalendarPermission()) {
                    logcat { "Tentativa de criar calendário sem a permissão WRITE_CALENDAR." }
                    return@withContext null
                }

                val accountName = "Kairos Imports"
                val accountType = CalendarContract.ACCOUNT_TYPE_LOCAL

                val values =
                    android.content.ContentValues().apply {
                        put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                        put(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                        put(CalendarContract.Calendars.NAME, name)
                        put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, name)
                        put(CalendarContract.Calendars.CALENDAR_COLOR, color)
                        put(
                            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                            CalendarContract.Calendars.CAL_ACCESS_OWNER,
                        )
                        put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
                        put(CalendarContract.Calendars.VISIBLE, 1)
                        put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                    }

                val uri = asSyncAdapter(CalendarContract.Calendars.CONTENT_URI, accountName, accountType)
                val resultUri = context.contentResolver.insert(uri, values)
                return@withContext resultUri?.lastPathSegment?.toLongOrNull()
            }

        override suspend fun updateCalendar(
            calendarId: Long,
            name: String,
            color: Int,
        ): Boolean =
            withContext(Dispatchers.IO) {
                if (!hasWriteCalendarPermission()) return@withContext false

                val accountName = "Kairos Imports"
                val accountType = CalendarContract.ACCOUNT_TYPE_LOCAL

                val values =
                    android.content.ContentValues().apply {
                        put(CalendarContract.Calendars.NAME, name)
                        put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, name)
                        put(CalendarContract.Calendars.CALENDAR_COLOR, color)
                    }

                val uri =
                    asSyncAdapter(
                        ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId),
                        accountName,
                        accountType,
                    )
                val rows = context.contentResolver.update(uri, values, null, null)
                return@withContext rows > 0
            }

        override suspend fun deleteCalendar(calendarId: Long): Boolean =
            withContext(Dispatchers.IO) {
                if (!hasWriteCalendarPermission()) return@withContext false

                val accountName = "Kairos Imports"
                val accountType = CalendarContract.ACCOUNT_TYPE_LOCAL

                val uri =
                    asSyncAdapter(
                        ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId),
                        accountName,
                        accountType,
                    )
                val rows = context.contentResolver.delete(uri, null, null)
                return@withContext rows > 0
            }

        private fun extractMeetLink(
            description: String?,
            location: String?,
        ): String? {
            if (description == null && location == null) return null
            val combinedText = "${description ?: ""} ${location ?: ""}"
            val pattern =
                """https?://(?:[a-zA-Z0-9-]+\.)*(?:meet\.google\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}
                ||zoom\.us/(?:j|my)/[^\s"'<>]+|teams\.microsoft\.com/l/meetup-join/[^\s"'<>]+
|webex\.com/(?:meet|join)/[^\s"'<>]+|join\.skype\.com/[a-zA-Z0-9]+|meet\.jit\.si/[^\s"'<>]+)
                """.trimMargin()
            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            return regex.find(combinedText)?.value
        }
    }
