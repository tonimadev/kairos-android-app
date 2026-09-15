package digital.tonima.core.ai.tools

import org.junit.Assert.assertNull
import org.junit.Test

class CreateEventToolTestExtra {
    private val tool = CreateEventTool()

    @Test
    fun `parseArguments returns null for missing required fields`() {
        val args = mapOf<String, Any?>("start_time" to 1000L)
        val result = tool.parseArguments(args)
        assertNull(result)
    }
}
