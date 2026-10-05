package digital.tonima.core.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusFilterPolicyTest {
    private fun hold(
        pkg: String = "com.whatsapp",
        category: String? = "msg",
        ongoing: Boolean = false,
        summary: Boolean = false,
    ) = FocusFilterPolicy.shouldHold(pkg, category, ongoing, summary, ownPackage = "digital.tonima.kairos")

    @Test
    fun `chat messages are held`() = assertTrue(hold())

    @Test
    fun `apps outside the allowlist are never touched`() = assertFalse(hold(pkg = "com.bank.app"))

    @Test
    fun `own notifications are never held`() =
        assertFalse(
            FocusFilterPolicy.shouldHold("com.whatsapp", "msg", false, false, ownPackage = "com.whatsapp"),
        )

    @Test
    fun `calls alarms and navigation are never held`() {
        listOf("call", "alarm", "reminder", "navigation", "transport").forEach {
            assertFalse("category $it", hold(category = it))
        }
    }

    @Test
    fun `ongoing notifications and group summaries are not held`() {
        assertFalse(hold(ongoing = true))
        assertFalse(hold(summary = true))
    }

    @Test
    fun `notifications without a category are held`() = assertTrue(hold(category = null))
}
