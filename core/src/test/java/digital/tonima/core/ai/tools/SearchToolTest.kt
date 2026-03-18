package digital.tonima.core.ai.tools

import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchToolTest {
    private val tool = SearchTool()

    @Test
    fun `name is search_events`() {
        assertEquals("search_events", tool.name)
    }

    @Test
    fun `riskLevel is SAFE`() {
        assertEquals(RiskLevel.SAFE, tool.riskLevel)
    }

    @Test
    fun `parseArguments returns SearchQueryChanged for valid query`() {
        val result = tool.parseArguments(mapOf("query" to "dentist"))

        assertNotNull(result)
        assertTrue(result is EventIntent.SearchQueryChanged)
        assertEquals("dentist", (result as EventIntent.SearchQueryChanged).query)
    }

    @Test
    fun `parseArguments returns null when query is missing`() {
        assertNull(tool.parseArguments(emptyMap()))
    }

    @Test
    fun `parseArguments returns null when query is blank`() {
        assertNull(tool.parseArguments(mapOf("query" to "  ")))
    }

    @Test
    fun `parseArguments returns null when query is null`() {
        assertNull(tool.parseArguments(mapOf("query" to null)))
    }
}
