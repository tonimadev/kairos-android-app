package digital.tonima.core.utils

import digital.tonima.kairos.core.model.Event
import logcat.LogPriority
import logcat.logcat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object IcsParser {
    fun parseIcs(content: String): List<Event> {
        val events = mutableListOf<Event>()
        // unfold lines (ICS can wrap lines with CRLF + space)
        val unfoldedContent = content.replace(Regex("\\r?\\n[ \t]"), "")
        val lines = unfoldedContent.lines()

        var inEvent = false
        var current = MutableEventState()

        for (line in lines) {
            val trimmedLine = line.trim()
            when {
                trimmedLine == "BEGIN:VEVENT" -> {
                    inEvent = true
                    current = MutableEventState()
                }
                trimmedLine == "END:VEVENT" -> {
                    if (inEvent) {
                        current.toEvent(events.size)?.let { events.add(it) }
                    }
                    inEvent = false
                }
                inEvent -> applyField(trimmedLine, current)
            }
        }

        return events
    }

    private fun applyField(
        trimmedLine: String,
        current: MutableEventState,
    ) {
        val splitIndex = trimmedLine.indexOf(':')
        if (splitIndex == -1) return

        val keyPart = trimmedLine.substring(0, splitIndex)
        val valuePart = trimmedLine.substring(splitIndex + 1)

        when {
            keyPart == "SUMMARY" -> current.title = valuePart
            keyPart.startsWith("DTSTART") -> {
                val (time, allDay) = parseTime(valuePart, extractTzid(keyPart))
                current.startTime = time
                if (allDay) current.isAllDay = true
            }
            keyPart.startsWith("DTEND") -> current.endTime = parseTime(valuePart, extractTzid(keyPart)).first
            keyPart == "LOCATION" -> current.location = valuePart
            keyPart == "UID" -> current.uid = valuePart
        }
    }

    private fun extractTzid(keyPart: String): String? =
        keyPart
            .split(";")
            .drop(1)
            .firstOrNull { it.startsWith("TZID=") }
            ?.substringAfter("TZID=")

    private fun parseTime(
        value: String,
        tzid: String?,
    ): Pair<Long, Boolean> =
        try {
            when {
                value.endsWith("Z") -> Pair(utcDateFormat().parse(value)?.time ?: 0L, false)
                value.length == 8 -> Pair(allDayDateFormat().parse(value)?.time ?: 0L, true)
                else -> Pair(localDateFormat(tzid).parse(value)?.time ?: 0L, false)
            }
        } catch (e: ParseException) {
            // Malformed date in the user's file: fall back to an unset time for this field.
            logcat(LogPriority.WARN) { "ICS: unparseable date '$value': ${e.message}" }
            Pair(0L, false)
        }

    private fun utcDateFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private fun allDayDateFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private fun localDateFormat(tzid: String?): SimpleDateFormat =
        SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply {
            timeZone = tzid?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()
        }

    private class MutableEventState {
        var title = ""
        var startTime = 0L
        var endTime = 0L
        var location: String? = null
        var isAllDay = false
        var uid = ""

        fun toEvent(eventIndex: Int): Event? {
            if (title.isEmpty() || startTime == 0L) return null
            val id =
                if (uid.isNotEmpty()) {
                    abs(uid.hashCode().toLong())
                } else {
                    System.currentTimeMillis() + eventIndex
                }
            return Event(
                id = id,
                title = title.replace("\\,", ",").replace("\\n", "\n"),
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
                location = location?.replace("\\,", ",")?.replace("\\n", "\n"),
            )
        }
    }
}
