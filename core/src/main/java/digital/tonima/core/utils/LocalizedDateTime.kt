package digital.tonima.core.utils

import android.content.Context
import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Date and time formats for what the user reads on screen. Hard-coded patterns such as `dd/MM/yyyy` or
 * `HH:mm` are wrong for most of the world (month-first dates, 12-hour clocks, native digits), so these
 * come from the platform's best pattern for the locale and respect the user's 12/24-hour setting.
 * Machine-facing text (AI prompts, ICS) must keep using fixed patterns instead.
 */
object LocalizedDateTime {
    /** Hour and minute, e.g. "15:30" or "3:30 PM". */
    fun time(
        locale: Locale,
        is24Hour: Boolean,
    ): DateTimeFormatter =
        formatter(locale, if (is24Hour) "Hm" else "hm") {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        }

    /** Short weekday and time, e.g. "Mon 15:30". */
    fun weekdayAndTime(
        locale: Locale,
        is24Hour: Boolean,
    ): DateTimeFormatter =
        formatter(locale, if (is24Hour) "EEEHm" else "EEEhm") {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        }

    /** Numeric date in the locale's order, e.g. "05/10/2026", "10/5/2026" or "2026/10/5". */
    fun numericDate(locale: Locale): DateTimeFormatter =
        formatter(locale, "yMd") { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }

    /** Numeric date followed by the time. */
    fun numericDateAndTime(
        locale: Locale,
        is24Hour: Boolean,
    ): DateTimeFormatter =
        formatter(locale, if (is24Hour) "yMdHm" else "yMdhm") {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        }

    fun time(context: Context): DateTimeFormatter = time(currentLocale(context), DateFormat.is24HourFormat(context))

    fun weekdayAndTime(context: Context): DateTimeFormatter =
        weekdayAndTime(currentLocale(context), DateFormat.is24HourFormat(context))

    fun numericDateAndTime(context: Context): DateTimeFormatter =
        numericDateAndTime(currentLocale(context), DateFormat.is24HourFormat(context))

    fun currentLocale(context: Context): Locale = context.resources.configuration.locales[0]

    // The platform pattern is ICU's; java.time rejects the few letters it does not know on older Android,
    // in which case the locale's plain short format is the safe fallback.
    private inline fun formatter(
        locale: Locale,
        skeleton: String,
        fallback: () -> DateTimeFormatter,
    ): DateTimeFormatter =
        try {
            DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
        } catch (e: IllegalArgumentException) {
            fallback().withLocale(locale)
        }
}
