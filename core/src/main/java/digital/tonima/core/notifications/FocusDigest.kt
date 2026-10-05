package digital.tonima.core.notifications

/** Counts how many notifications each app sent during a meeting. Keeps only counts, never content. */
class FocusDigest {
    private val counts = linkedMapOf<String, Int>()

    val total: Int get() = counts.values.sum()

    fun add(appLabel: String) {
        counts[appLabel] = (counts[appLabel] ?: 0) + 1
    }

    /** Apps ordered by most notifications first. */
    fun summary(): List<Pair<String, Int>> = counts.entries.sortedByDescending { it.value }.map { it.key to it.value }

    fun isEmpty(): Boolean = counts.isEmpty()

    fun clear() = counts.clear()
}
