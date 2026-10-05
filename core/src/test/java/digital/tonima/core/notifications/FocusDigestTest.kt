package digital.tonima.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusDigestTest {
    @Test
    fun `counts per app ordered by most notifications`() {
        val digest = FocusDigest()
        repeat(2) { digest.add("Gmail") }
        repeat(3) { digest.add("WhatsApp") }

        assertEquals(listOf("WhatsApp" to 3, "Gmail" to 2), digest.summary())
        assertEquals(5, digest.total)
    }

    @Test
    fun `clear forgets everything`() {
        val digest = FocusDigest().apply { add("Gmail") }

        digest.clear()

        assertTrue(digest.isEmpty())
        assertEquals(0, digest.total)
    }
}
