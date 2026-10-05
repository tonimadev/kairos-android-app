package digital.tonima.core.notifications

/** Remembers (in memory only) which suggestions were already shown so the same invite is offered once. */
class SuggestionTracker(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    private val seen = linkedMapOf<String, Long>()

    @Synchronized
    fun shouldSuggest(
        key: String,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        seen.entries.removeAll { now - it.value > ttlMs }
        if (key in seen) return false
        seen[key] = now
        while (seen.size > maxEntries) seen.remove(seen.keys.first())
        return true
    }

    private companion object {
        const val DEFAULT_TTL_MS = 6 * 60 * 60 * 1000L
        const val DEFAULT_MAX_ENTRIES = 50
    }
}
