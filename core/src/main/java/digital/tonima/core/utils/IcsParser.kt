package digital.tonima.core.utils

import digital.tonima.core.model.Event
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
        var currentTitle = ""
        var currentStartTime = 0L
        var currentEndTime = 0L
        var currentLocation: String? = null
        var isAllDay = false
        var uid = ""

        val dateFormatUtc =
            SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        val dateFormatLocal =
            SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
        val dateFormatAllDay =
            SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

        fun parseTime(value: String): Pair<Long, Boolean> {
            var result = Pair(0L, false)
            try {
                if (value.endsWith("Z")) {
                    result = Pair(dateFormatUtc.parse(value)?.time ?: 0L, false)
                } else if (value.length == 8) {
                    result = Pair(dateFormatAllDay.parse(value)?.time ?: 0L, true)
                } else {
                    result = Pair(dateFormatLocal.parse(value)?.time ?: 0L, false)
                }
            } catch (e: Exception) {
                // Ignore parsing errors and return default
            }
            return result
        }

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine == "BEGIN:VEVENT") {
                inEvent = true
                currentTitle = ""
                currentStartTime = 0L
                currentEndTime = 0L
                currentLocation = null
                isAllDay = false
                uid = ""
            } else if (trimmedLine == "END:VEVENT") {
                if (inEvent && currentTitle.isNotEmpty() && currentStartTime != 0L) {
                    val id =
                        if (uid.isNotEmpty()) {
                            abs(uid.hashCode().toLong())
                        } else {
                            System.currentTimeMillis() + events.size
                        }
                    events.add(
                        Event(
                            id = id,
                            title = currentTitle.replace("\\,", ",").replace("\\n", "\n"),
                            startTime = currentStartTime,
                            endTime = currentEndTime,
                            isAllDay = isAllDay,
                            location = currentLocation?.replace("\\,", ",")?.replace("\\n", "\n"),
                        ),
                    )
                }
                inEvent = false
            } else if (inEvent) {
                val splitIndex = trimmedLine.indexOf(':')
                if (splitIndex == -1) continue

                val keyPart = trimmedLine.substring(0, splitIndex)
                val valuePart = trimmedLine.substring(splitIndex + 1)

                if (keyPart == "SUMMARY") {
                    currentTitle = valuePart
                } else if (keyPart.startsWith("DTSTART")) {
                    val parsed = parseTime(valuePart)
                    currentStartTime = parsed.first
                    if (parsed.second) isAllDay = true
                } else if (keyPart.startsWith("DTEND")) {
                    val parsed = parseTime(valuePart)
                    currentEndTime = parsed.first
                } else if (keyPart == "LOCATION") {
                    currentLocation = valuePart
                } else if (keyPart == "UID") {
                    uid = valuePart
                }
            }
        }

        return events
    }
}
