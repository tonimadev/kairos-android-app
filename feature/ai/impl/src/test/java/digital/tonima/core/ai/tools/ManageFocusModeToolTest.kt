package digital.tonima.core.ai.tools

import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ManageFocusModeToolTest {
    private val tool = ManageFocusModeTool()

    @Test
    fun `name is manage_focus_mode`() {
        assertEquals("manage_focus_mode", tool.name)
    }

    @Test
    fun `riskLevel is MODERATE`() {
        assertEquals(RiskLevel.MODERATE, tool.riskLevel)
    }

    @Test
    fun `parseArguments returns ToggleFocusMode intent for valid args`() {
        val args = mapOf("enabled" to true)
        val result = tool.parseArguments(args)

        assertNotNull(result)
        val intent = result as EventIntent.ToggleFocusMode
        assertEquals(true, intent.enabled)
    }

    @Test
    fun `parseArguments returns ToggleFocusMode with false for disabled`() {
        val args = mapOf("enabled" to false)
        val result = tool.parseArguments(args)

        assertNotNull(result)
        val intent = result as EventIntent.ToggleFocusMode
        assertEquals(false, intent.enabled)
    }

    @Test
    fun `parseArguments returns null for missing enabled`() {
        val args = emptyMap<String, Any?>()
        assertNull(tool.parseArguments(args))
    }
}
