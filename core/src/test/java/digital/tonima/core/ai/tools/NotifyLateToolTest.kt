package digital.tonima.core.ai.tools

import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotifyLateToolTest {
    private val tool = NotifyLateTool()

    @Test
    fun `name is notify_late`() {
        assertEquals("notify_late", tool.name)
    }

    @Test
    fun `riskLevel is MODERATE`() {
        assertEquals(RiskLevel.MODERATE, tool.riskLevel)
    }

    @Test
    fun `parseArguments returns NotifyRunningLate intent for valid args`() {
        val args =
            mapOf(
                "event_id" to "123",
                "message" to "Stuck in traffic",
            )

        val result = tool.parseArguments(args)

        assertNotNull(result)
        val intent = result as EventIntent.NotifyRunningLate
        assertEquals("123", intent.eventId)
        assertEquals("Stuck in traffic", intent.message)
    }

    @Test
    fun `parseArguments returns null for missing event_id`() {
        val args =
            mapOf(
                "message" to "Stuck in traffic",
            )
        assertNull(tool.parseArguments(args))
    }

    @Test
    fun `parseArguments returns null for missing message`() {
        val args =
            mapOf(
                "event_id" to "123",
            )
        assertNull(tool.parseArguments(args))
    }
}
