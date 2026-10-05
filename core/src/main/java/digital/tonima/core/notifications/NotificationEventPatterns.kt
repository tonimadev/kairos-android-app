package digital.tonima.core.notifications

import java.time.DayOfWeek
import java.time.LocalDate

/** An hour/minute found in the text, already adjusted for am/pm, with where it was found. */
internal class TimeHit(
    val hour: Int,
    val minute: Int,
    val range: IntRange,
    val explicitMeridiem: Boolean,
    val hourWasAdjusted: Boolean = false,
)

/** A date found in the text. [weekly] dates come from a weekday name and may roll over to next week. */
internal class DateHit(
    val date: LocalDate,
    val range: IntRange,
    val weekly: Boolean = false,
)

/** All the regexes the parser uses, built once from the vocabularies of every supported language. */
internal object NotificationEventPatterns {
    private val vocabularies = NotificationVocabularies.ALL

    private fun foldedMap(entries: List<Pair<List<String>, Int>>): Map<String, Int> =
        entries.flatMap { (words, value) -> words.map { TextFolding.fold(it) to value } }.toMap()

    val intent = Regex(TextFolding.alternation(vocabularies.flatMap { it.intent }, wholeWord = false))

    val relativeDays: Map<String, Int> =
        foldedMap(
            vocabularies.map { it.today to 0 } + vocabularies.map { it.tomorrow to 1 },
        )
    val relative = Regex(TextFolding.alternation(relativeDays.keys, wholeWord = true))

    val weekdays: Map<String, DayOfWeek> =
        vocabularies.flatMap { vocabulary ->
            vocabulary.weekdays.flatMapIndexed { index, names ->
                names.map { TextFolding.fold(it) to DayOfWeek.of(index + 1) }
            }
        }.toMap()
    val weekday = Regex(TextFolding.alternation(weekdays.keys, wholeWord = false))

    val months: Map<String, Int> =
        vocabularies.flatMap { vocabulary ->
            vocabulary.months.flatMapIndexed { index, names -> names.map { TextFolding.fold(it) to index + 1 } }
        }.toMap()
    private val monthNames = TextFolding.alternation(months.keys, wholeWord = true)

    /** "20 de outubro", "20 octobre 2026", "20. Oktober", "20th of May". */
    val dayThenMonth =
        Regex(
            "(?<!\\d)(\\d{1,2})(?:st|nd|rd|th|º|°)?\\.?\\s*(?:de |del |of |d')?\\s*($monthNames)" +
                "(?:\\s*(?:de |of |,)?\\s*(\\d{4})(?!\\d))?",
        )

    /** "October 20", "Oct 20th, 2026". */
    val monthThenDay =
        Regex("($monthNames)\\.?\\s*(\\d{1,2})(?:st|nd|rd|th)?(?!\\d)(?:,?\\s*(\\d{4})(?!\\d))?")

    /** "15/10", "15/10/2026", "15.10.2026" (a dotted date needs a year: "3.5" is more likely a number). */
    val numericDate = Regex("(?<![\\d.])(\\d{1,2})([/.])(\\d{1,2})(?:\\2(\\d{2,4}))?(?!\\d)")
    val isoDate = Regex("(?<!\\d)(\\d{4})-(\\d{2})-(\\d{2})(?!\\d)")

    /** "10月20日" (Japanese/Chinese), optionally with the year. */
    val eastAsianDate = Regex("(?:(\\d{4})年)?(\\d{1,2})月(\\d{1,2})[日号]")

    private const val NOT_AFTER_DIGIT = "(?<![\\d:.])"

    val clock = Regex("$NOT_AFTER_DIGIT([01]?\\d|2[0-3]):([0-5]\\d)(?!\\d)(?:\\s?(am|pm)(?!\\p{L}))?")
    val hourMark = Regex("$NOT_AFTER_DIGIT([01]?\\d|2[0-3])\\s?h(?:\\s?([0-5]\\d))?(?![\\p{L}\\d])")
    val meridiem = Regex("$NOT_AFTER_DIGIT(1[0-2]|0?[1-9])\\s?(am|pm)(?!\\p{L})")
    val uhr = Regex("$NOT_AFTER_DIGIT([01]?\\d|2[0-3])(?:[.:]([0-5]\\d))?\\s?uhr(?!\\p{L})")
    val eastAsianTime = Regex("$NOT_AFTER_DIGIT(\\d{1,2})[時时点點](?:(\\d{1,2})分?|半)?")
    val hindiTime = Regex("$NOT_AFTER_DIGIT(\\d{1,2})\\s?बजे")

    /** A bare hour ("3 da tarde"); only trusted when a part of the day sits next to it. */
    val bareHour = Regex("$NOT_AFTER_DIGIT(1[0-2]|0?[1-9])(?![\\d:.])")
    val arabicTime = Regex("الساعة\\s?(\\d{1,2})(?::([0-5]\\d))?")

    private val afternoonWords = vocabularies.flatMap { it.afternoon }.map { TextFolding.fold(it) }
    private val morningWords = vocabularies.flatMap { it.morning }.map { TextFolding.fold(it) }

    fun isAfternoon(window: String): Boolean = afternoonWords.any { window.contains(it) }

    fun isMorning(window: String): Boolean = morningWords.any { window.contains(it) }

    /** Prepositions/articles left dangling at the edges of a title once the date and time are removed. */
    val fillers: Regex =
        vocabularies.flatMap { it.fillers }.distinct().sortedByDescending { it.length }
            .joinToString("|") { Regex.escape(it) }
            .let { words ->
                Regex("^(?:(?:$words)\\s+)+|(?:\\s+(?:$words))+$", RegexOption.IGNORE_CASE)
            }
}
