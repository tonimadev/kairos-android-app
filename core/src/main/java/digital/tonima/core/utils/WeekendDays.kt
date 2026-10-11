package digital.tonima.core.utils

import android.icu.util.Calendar
import android.icu.util.ULocale
import com.paulrybitskyi.hiltbinder.BindType
import logcat.LogPriority
import logcat.logcat
import java.time.DayOfWeek
import java.util.Locale
import java.util.MissingResourceException
import javax.inject.Inject

/** The days that make up the weekend where the user lives. */
fun interface WeekendDaysProvider {
    fun weekendDays(): Set<DayOfWeek>
}

@BindType(installIn = BindType.Component.SINGLETON, to = WeekendDaysProvider::class)
class RegionWeekendDaysProvider
    @Inject
    constructor() : WeekendDaysProvider {
        override fun weekendDays(): Set<DayOfWeek> = WeekendDays.forLocale(Locale.getDefault())
    }

/**
 * The weekend is not Saturday and Sunday everywhere: it is Friday in Iran, Thursday and Friday in
 * Afghanistan, Friday and Saturday in Algeria, Egypt, Iraq or Sudan, and only Sunday in India. The
 * days come from the region's CLDR week data. A locale without a region (e.g. "fa") uses the most
 * likely region for its language.
 */
object WeekendDays {
    private val DEFAULT = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun forLocale(locale: Locale): Set<DayOfWeek> =
        try {
            val region = ULocale.addLikelySubtags(ULocale.forLocale(locale)).country
            val weekData = Calendar.getWeekDataForRegion(region)
            daysBetween(weekData.weekendOnset.toDayOfWeek(), weekData.weekendCease.toDayOfWeek())
        } catch (e: IllegalArgumentException) {
            logcat(LogPriority.WARN) { "No week data for $locale, using Saturday and Sunday: ${e.message}" }
            DEFAULT
        } catch (e: MissingResourceException) {
            logcat(LogPriority.WARN) { "No week data for $locale, using Saturday and Sunday: ${e.message}" }
            DEFAULT
        }

    /** ICU numbers the days from Sunday = 1 to Saturday = 7. */
    private fun Int.toDayOfWeek(): DayOfWeek = DayOfWeek.SUNDAY.plus((this - Calendar.SUNDAY).toLong())

    private fun daysBetween(
        first: DayOfWeek,
        last: DayOfWeek,
    ): Set<DayOfWeek> = generateSequence(first) { if (it == last) null else it.plus(1) }.toSet()
}
