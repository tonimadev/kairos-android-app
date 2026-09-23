package digital.tonima.core.ai.tools

import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.di.AIToolsModule
import digital.tonima.core.viewmodel.AiIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The model only sees a tool through its name, description and parameters schema, so every
 * registered tool must declare them consistently or Gemini will call it with the wrong arguments.
 */
class AiToolContractTest {
    private val tools: List<AITool> =
        with(AIToolsModule) {
            listOf(
                provideCreateEventTool(),
                provideToggleGlobalAlarmsTool(),
                provideSearchTool(),
                provideNotifyLateTool(),
                provideManageFocusModeTool(),
                provideRescheduleEventTool(),
                provideCategorizeEventTool(),
                provideSuggestFocusBlocksTool(),
                provideAnalyzeScheduleTool(),
            )
        }

    @Test
    fun `every tool has a unique name and a description`() {
        assertEquals(tools.size, tools.map { it.name }.toSet().size)
        tools.forEach { assertTrue(it.name, it.description.isNotBlank()) }
    }

    @Test
    fun `every required parameter is declared with a supported type`() {
        val supportedTypes = setOf("string", "number", "integer", "boolean")
        tools.forEach { tool ->
            val schema = tool.parametersSchema
            assertEquals(tool.name, "object", schema["type"])

            @Suppress("UNCHECKED_CAST")
            val properties = schema["properties"] as Map<String, Map<String, Any>>

            @Suppress("UNCHECKED_CAST")
            val required = schema["required"] as? List<String> ?: emptyList()

            assertTrue(tool.name, properties.keys.containsAll(required))
            properties.forEach { (key, definition) ->
                assertTrue("${tool.name}.$key", definition["type"] in supportedTypes)
            }
        }
    }

    @Test
    fun `analyze schedule needs a timeframe`() {
        val tool = AnalyzeScheduleTool()

        assertEquals(AiIntent.AnalyzeSchedule("week"), tool.parseArguments(mapOf("timeframe" to "week")))
        assertNull(tool.parseArguments(emptyMap()))
    }

    @Test
    fun `tools reject calls with missing arguments`() {
        assertNull(RescheduleEventTool().parseArguments(mapOf("event_id" to "1", "new_start_time" to 1L)))
        assertNull(RescheduleEventTool().parseArguments(mapOf("new_start_time" to 1L, "new_end_time" to 2L)))
        assertNull(CategorizeEventTool().parseArguments(mapOf("event_id" to "1")))
        assertNull(CategorizeEventTool().parseArguments(mapOf("category" to "Work")))
        assertNull(SuggestFocusBlocksTool().parseArguments(mapOf("start_time" to 1L)))
        assertNull(SuggestFocusBlocksTool().parseArguments(mapOf("end_time" to 2L)))
    }
}
