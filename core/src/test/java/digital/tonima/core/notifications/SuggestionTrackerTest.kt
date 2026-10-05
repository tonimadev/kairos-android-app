package digital.tonima.core.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionTrackerTest {
    @Test
    fun `the same suggestion is only offered once`() {
        val tracker = SuggestionTracker()

        assertTrue(tracker.shouldSuggest("a", now = 0))
        assertFalse(tracker.shouldSuggest("a", now = 1))
        assertTrue(tracker.shouldSuggest("b", now = 2))
    }

    @Test
    fun `a suggestion can come back after the memory expires`() {
        val tracker = SuggestionTracker(ttlMs = 100)

        assertTrue(tracker.shouldSuggest("a", now = 0))
        assertTrue(tracker.shouldSuggest("a", now = 101))
    }

    @Test
    fun `memory is bounded`() {
        val tracker = SuggestionTracker(maxEntries = 2)

        tracker.shouldSuggest("a", now = 0)
        tracker.shouldSuggest("b", now = 1)
        tracker.shouldSuggest("c", now = 2)

        assertTrue("oldest entry was evicted", tracker.shouldSuggest("a", now = 3))
    }
}
