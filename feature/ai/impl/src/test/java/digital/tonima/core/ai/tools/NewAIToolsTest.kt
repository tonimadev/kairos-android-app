package digital.tonima.core.ai.tools

import digital.tonima.core.viewmodel.AiIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewAIToolsTest {
    @Test
    fun `RescheduleEventTool parses arguments correctly`() {
        val tool = RescheduleEventTool()
        val args =
            mapOf(
                "event_id" to "evt123",
                "new_start_time" to 1715558400000L,
                "new_end_time" to 1715562000000L,
            )
        val result = tool.parseArguments(args)

        assertTrue(result is AiIntent.RescheduleEvent)
        val intent = result as AiIntent.RescheduleEvent
        assertEquals("evt123", intent.eventId)
        assertEquals(1715558400000L, intent.newStartTime)
        assertEquals(1715562000000L, intent.newEndTime)
    }

    @Test
    fun `CategorizeEventTool parses arguments correctly`() {
        val tool = CategorizeEventTool()
        val args =
            mapOf(
                "event_id" to "evt456",
                "category" to "Work",
            )
        val result = tool.parseArguments(args)

        assertTrue(result is AiIntent.CategorizeEvent)
        val intent = result as AiIntent.CategorizeEvent
        assertEquals("evt456", intent.eventId)
        assertEquals("Work", intent.category)
    }

    @Test
    fun `SuggestFocusBlocksTool parses arguments correctly`() {
        val tool = SuggestFocusBlocksTool()
        val args =
            mapOf(
                "start_time" to 1715558400000L,
                "end_time" to 1715562000000L,
            )
        val result = tool.parseArguments(args)

        assertTrue(result is AiIntent.CreateFocusBlock)
        val intent = result as AiIntent.CreateFocusBlock
        assertEquals(1715558400000L, intent.startTime)
        assertEquals(1715562000000L, intent.endTime)
    }
}
