package digital.tonima.core.ai.tools

import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleGlobalAlarmsToolTest {
    private val tool = ToggleGlobalAlarmsTool()

    @Test
    fun `name is toggle_global_alarms`() {
        assertEquals("toggle_global_alarms", tool.name)
    }

    @Test
    fun `riskLevel is MODERATE`() {
        assertEquals(RiskLevel.MODERATE, tool.riskLevel)
    }

    @Test
    fun `parseArguments returns ToggleGlobalAlarms true`() {
        val result = tool.parseArguments(mapOf("enabled" to true))

        assertNotNull(result)
        assertTrue(result is EventIntent.ToggleGlobalAlarms)
        assertTrue((result as EventIntent.ToggleGlobalAlarms).enabled)
    }

    @Test
    fun `parseArguments returns ToggleGlobalAlarms false`() {
        val result = tool.parseArguments(mapOf("enabled" to false))

        assertNotNull(result)
        assertEquals(false, (result as EventIntent.ToggleGlobalAlarms).enabled)
    }

    @Test
    fun `parseArguments returns null when enabled is missing`() {
        assertNull(tool.parseArguments(emptyMap()))
    }

    @Test
    fun `parseArguments returns null when enabled is not a boolean`() {
        assertNull(tool.parseArguments(mapOf("enabled" to "yes")))
    }
}
