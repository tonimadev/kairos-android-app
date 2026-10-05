package digital.tonima.core.notifications

import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class SuggestedEvent(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
)

/**
 * Looks for "something that sounds like an appointment" in a notification's text: an intent word
 * (meeting, lunch, consulta, 会議…), a date and a time. Runs entirely on-device and understands the 10
 * languages the app is translated to (see [NotificationVocabularies]); the language of the text is not
 * known in advance, so every vocabulary is tried. Returns null when unsure so that no noisy or wrong
 * suggestion is shown.
 */
object NotificationEventParser {
    private const val MAX_TITLE_LENGTH = 60
    private const val MAX_DAYS_AHEAD = 366L
    private const val DEFAULT_TITLE = "Evento"
    private const val NOON = 12
    private const val MAX_MONTH = 12
    private const val HALF_HOUR = 30
    private const val DAYPART_WINDOW = 24
    private const val TWO_DIGIT_YEAR_BASE = 2000
    private val DEFAULT_DURATION: Duration = Duration.ofHours(1)
    private val TITLE_EDGE_PUNCTUATION = "?!.,;:-–—،。、"

    fun parse(
        text: String,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): SuggestedEvent? {
        val folded = TextFolding.fold(text)
        if (!NotificationEventPatterns.intent.containsMatchIn(folded)) return null
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val date = findDate(folded, today, isMonthFirst(locale))
        val time = findTime(folded, date?.range)
        val start = if (date != null && time != null) startOf(date, time, zone, now) else null
        return if (date != null && time != null && start != null) {
            SuggestedEvent(
                title = cleanTitle(text, listOf(date.range, time.range)),
                startMillis = start.toEpochMilli(),
                endMillis = start.plus(DEFAULT_DURATION).toEpochMilli(),
            )
        } else {
            null
        }
    }

    /** Whether this locale writes the month before the day in short dates (en-US: 10/20, ja: 10/20). */
    private fun isMonthFirst(locale: Locale): Boolean {
        val pattern =
            DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.SHORT,
                null,
                IsoChronology.INSTANCE,
                locale,
            )
        val month = pattern.indexOf('M')
        val day = pattern.indexOf('d')
        return month in 0 until (if (day < 0) Int.MAX_VALUE else day)
    }

    /**
     * The start instant, or null when it is in the past or too far ahead to be a plausible invite.
     * A weekday name that already passed today means the same weekday next week.
     */
    private fun startOf(
        date: DateHit,
        time: TimeHit,
        zone: ZoneId,
        now: Long,
    ): Instant? {
        val nowInstant = Instant.ofEpochMilli(now)
        val limit = nowInstant.plus(Duration.ofDays(MAX_DAYS_AHEAD))
        var start = date.date.atTime(time.hour, time.minute).atZone(zone).toInstant()
        if (date.weekly && !start.isAfter(nowInstant)) {
            start = date.date.plusWeeks(1).atTime(time.hour, time.minute).atZone(zone).toInstant()
        }
        return start.takeIf { it.isAfter(nowInstant) && !it.isAfter(limit) }
    }

    // ── Dates ─────────────────────────────────────────────────────────────────────────────

    private fun findDate(
        text: String,
        today: LocalDate,
        monthFirst: Boolean,
    ): DateHit? =
        listOfNotNull(
            relativeDate(text, today),
            weekdayDate(text, today),
            dayThenMonth(text, today),
            monthThenDay(text, today),
            numericDate(text, today, monthFirst),
            isoDate(text),
            eastAsianDate(text, today),
        ).minWithOrNull(compareBy<DateHit> { it.range.first }.thenByDescending { it.range.last })

    private fun relativeDate(
        text: String,
        today: LocalDate,
    ): DateHit? =
        NotificationEventPatterns.relative.find(text)?.let { match ->
            val offset = NotificationEventPatterns.relativeDays[match.value] ?: return null
            DateHit(today.plusDays(offset.toLong()), match.range)
        }

    private fun weekdayDate(
        text: String,
        today: LocalDate,
    ): DateHit? =
        NotificationEventPatterns.weekday.find(text)?.let { match ->
            val day = NotificationEventPatterns.weekdays[match.value] ?: return null
            DateHit(today.with(TemporalAdjusters.nextOrSame(day)), match.range, weekly = true)
        }

    private fun dayThenMonth(
        text: String,
        today: LocalDate,
    ): DateHit? =
        NotificationEventPatterns.dayThenMonth.findAll(text).firstNotNullOfOrNull { match ->
            val (day, monthName, year) = match.destructured
            val month = NotificationEventPatterns.months[monthName] ?: return@firstNotNullOfOrNull null
            calendarDate(day.toInt(), month, year.toIntOrNull(), today)?.let { DateHit(it, match.range) }
        }

    private fun monthThenDay(
        text: String,
        today: LocalDate,
    ): DateHit? =
        NotificationEventPatterns.monthThenDay.findAll(text).firstNotNullOfOrNull { match ->
            val (monthName, day, year) = match.destructured
            val month = NotificationEventPatterns.months[monthName] ?: return@firstNotNullOfOrNull null
            calendarDate(day.toInt(), month, year.toIntOrNull(), today)?.let { DateHit(it, match.range) }
        }

    private fun numericDate(
        text: String,
        today: LocalDate,
        monthFirst: Boolean,
    ): DateHit? =
        NotificationEventPatterns.numericDate.findAll(text).firstNotNullOfOrNull { match ->
            val (first, separator, second, yearText) = match.destructured
            val year = yearText.toIntOrNull()?.let { if (it < 100) it + TWO_DIGIT_YEAR_BASE else it }
            // "3.5" is more likely a number than a date; a dotted date needs a year.
            if (separator == "." && year == null) return@firstNotNullOfOrNull null
            val (day, month) =
                dayAndMonth(first.toInt(), second.toInt(), monthFirst) ?: return@firstNotNullOfOrNull null
            calendarDate(day, month, year, today)?.let { DateHit(it, match.range) }
        }

    /** A number above 12 can only be the day; when both could be, the locale's order decides. */
    private fun dayAndMonth(
        first: Int,
        second: Int,
        monthFirst: Boolean,
    ): Pair<Int, Int>? =
        when {
            first > MAX_MONTH && second > MAX_MONTH -> null
            first > MAX_MONTH -> first to second
            second > MAX_MONTH -> second to first
            monthFirst -> second to first
            else -> first to second
        }

    private fun isoDate(text: String): DateHit? =
        NotificationEventPatterns.isoDate.find(text)?.let { match ->
            val (year, month, day) = match.destructured
            calendarDate(day.toInt(), month.toInt(), year.toInt(), null)?.let { DateHit(it, match.range) }
        }

    private fun eastAsianDate(
        text: String,
        today: LocalDate,
    ): DateHit? =
        NotificationEventPatterns.eastAsianDate.find(text)?.let { match ->
            val (year, month, day) = match.destructured
            calendarDate(day.toInt(), month.toInt(), year.toIntOrNull(), today)?.let { DateHit(it, match.range) }
        }

    /** A real calendar date; without a year, the next time that day comes around. */
    private fun calendarDate(
        day: Int,
        month: Int,
        year: Int?,
        today: LocalDate?,
    ): LocalDate? =
        try {
            val date = LocalDate.of(year ?: today?.year ?: return null, month, day)
            if (year == null && today != null && date.isBefore(today)) date.plusYears(1) else date
        } catch (e: DateTimeException) {
            null // not a real calendar date (e.g. 31/02)
        }

    // ── Times ─────────────────────────────────────────────────────────────────────────────

    private fun findTime(
        text: String,
        dateRange: IntRange?,
    ): TimeHit? = explicitTime(text) ?: bareHourWithDaypart(text, dateRange)

    /** "3 da tarde", "3 in the afternoon": no clock mark, but the part of the day says which hour is meant. */
    private fun bareHourWithDaypart(
        text: String,
        dateRange: IntRange?,
    ): TimeHit? =
        NotificationEventPatterns.bareHour.findAll(text)
            .filter { dateRange == null || it.range.first !in dateRange }
            .mapNotNull { timeHit(it, hour = 1, text = text) }
            .firstOrNull { it.hourWasAdjusted }

    private fun explicitTime(text: String): TimeHit? =
        with(NotificationEventPatterns) {
            listOfNotNull(
                clock.find(text)?.let { timeHit(it, hour = 1, minute = 2, meridiem = 3, text = text) },
                hourMark.find(text)?.let { timeHit(it, hour = 1, minute = 2, text = text) },
                meridiem.find(text)?.let { timeHit(it, hour = 1, meridiem = 2, text = text) },
                uhr.find(text)?.let { timeHit(it, hour = 1, minute = 2, text = text) },
                eastAsianTime.find(text)?.let { timeHit(it, hour = 1, minute = 2, text = text) },
                hindiTime.find(text)?.let { timeHit(it, hour = 1, text = text) },
                arabicTime.find(text)?.let { timeHit(it, hour = 1, minute = 2, text = text) },
            )
        }.minByOrNull { it.range.first }

    private fun timeHit(
        match: MatchResult,
        hour: Int,
        text: String,
        minute: Int = 0,
        meridiem: Int = 0,
    ): TimeHit? {
        val parsedHour = match.groupValues[hour].toIntOrNull() ?: return null
        val parsedMinute =
            when {
                match.value.endsWith("半") -> HALF_HOUR
                minute > 0 -> match.groupValues[minute].toIntOrNull() ?: 0
                else -> 0
            }
        val marker = if (meridiem > 0) match.groupValues[meridiem] else ""
        val adjusted =
            when {
                marker == "pm" -> if (parsedHour < NOON) parsedHour + NOON else parsedHour
                marker == "am" -> if (parsedHour == NOON) 0 else parsedHour
                else -> adjustForDaypart(parsedHour, text, match.range)
            }
        return if (adjusted in 0..HOURS_MAX && parsedMinute in 0..MINUTES_MAX) {
            TimeHit(adjusted, parsedMinute, match.range, marker.isNotEmpty(), adjusted != parsedHour)
        } else {
            null
        }
    }

    /** "3 da tarde", "下午3点", "3 вечера": a part of the day next to a 12-hour time moves it to the right half. */
    private fun adjustForDaypart(
        hour: Int,
        text: String,
        range: IntRange,
    ): Int {
        if (hour > NOON) return hour
        val window =
            text.substring(
                maxOf(0, range.first - DAYPART_WINDOW),
                minOf(text.length, range.last + DAYPART_WINDOW),
            )
        val afternoon = NotificationEventPatterns.isAfternoon(window)
        val morning = NotificationEventPatterns.isMorning(window)
        return when {
            afternoon && !morning -> if (hour < NOON) hour + NOON else hour
            morning && !afternoon -> if (hour == NOON) 0 else hour
            else -> hour
        }
    }

    // ── Title ─────────────────────────────────────────────────────────────────────────────

    private fun cleanTitle(
        text: String,
        ranges: List<IntRange>,
    ): String {
        val builder = StringBuilder(text)
        ranges.sortedByDescending { it.first }.forEach { builder.delete(it.first, it.last + 1) }
        val cleaned =
            builder.toString()
                .replace(Regex("\\s+"), " ")
                .trim { it in TITLE_EDGE_PUNCTUATION || it.isWhitespace() }
                .replace(NotificationEventPatterns.fillers, "")
                .trim { it in TITLE_EDGE_PUNCTUATION || it.isWhitespace() }
        return cleaned.ifBlank { DEFAULT_TITLE }.take(MAX_TITLE_LENGTH).trim()
    }

    private const val HOURS_MAX = 23
    private const val MINUTES_MAX = 59
}
