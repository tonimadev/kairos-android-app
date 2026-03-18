package digital.tonima.core.ai

import digital.tonima.core.viewmodel.EventIntent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionRegistryTest {
    private fun createTool(
        name: String,
        risk: RiskLevel = RiskLevel.SAFE,
        intent: EventIntent? = EventIntent.RefreshEvents,
    ): AITool {
        val tool = mockk<AITool>(relaxed = true)
        every { tool.name } returns name
        every { tool.riskLevel } returns risk
        every { tool.description } returns "desc"
        every { tool.parametersSchema } returns emptyMap()
        every { tool.parseArguments(any()) } returns intent
        return tool
    }

    @Test
    fun `processAIToolCall returns Success when tool found and args valid`() {
        val tool = createTool("search_events")
        val registry = ActionRegistry(setOf(tool))

        val result = registry.processAIToolCall("search_events", mapOf("q" to "test"))

        assertTrue(result is AIToolResult.Success)
        val success = result as AIToolResult.Success
        assertEquals(tool, success.tool)
        assertEquals(EventIntent.RefreshEvents, success.intent)
    }

    @Test
    fun `processAIToolCall returns ToolNotFound for unknown tool`() {
        val registry = ActionRegistry(setOf(createTool("search_events")))

        val result = registry.processAIToolCall("unknown_tool", emptyMap())

        assertTrue(result is AIToolResult.ToolNotFound)
        assertEquals("unknown_tool", (result as AIToolResult.ToolNotFound).toolName)
    }

    @Test
    fun `processAIToolCall returns InvalidArguments when parseArguments returns null`() {
        val tool = createTool("bad_tool", intent = null)
        val registry = ActionRegistry(setOf(tool))

        val args = mapOf<String, Any?>("x" to "y")
        val result = registry.processAIToolCall("bad_tool", args)

        assertTrue(result is AIToolResult.InvalidArguments)
        val invalid = result as AIToolResult.InvalidArguments
        assertEquals("bad_tool", invalid.toolName)
        assertEquals(args, invalid.args)
    }

    @Test
    fun `availableToolDescriptors returns descriptors for all registered tools`() {
        val tools =
            setOf(
                createTool("tool_a"),
                createTool("tool_b"),
            )
        val registry = ActionRegistry(tools)

        val descriptors = registry.availableToolDescriptors()

        assertEquals(2, descriptors.size)
        val names = descriptors.map { it["name"] }.toSet()
        assertTrue(names.contains("tool_a"))
        assertTrue(names.contains("tool_b"))
    }

    @Test
    fun `registeredTools returns the injected tool set`() {
        val tools = setOf(createTool("a"), createTool("b"))
        val registry = ActionRegistry(tools)

        assertEquals(tools, registry.registeredTools())
    }
}
