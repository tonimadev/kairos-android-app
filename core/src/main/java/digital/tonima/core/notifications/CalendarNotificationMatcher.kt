package digital.tonima.core.notifications

import java.text.Normalizer

/** Decides whether a calendar app's notification is the reminder of an event Kairos already rang for. */
object CalendarNotificationMatcher {
    private const val MIN_TITLE_LENGTH = 3
    private val diacritics = Regex("\\p{InCombiningDiacriticalMarks}+")

    fun isSameEvent(
        notificationTexts: List<String?>,
        eventTitle: String,
    ): Boolean {
        val title = normalize(eventTitle)
        if (title.length < MIN_TITLE_LENGTH) return false
        return notificationTexts.any { text ->
            text != null && normalize(text).contains(title)
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
}
