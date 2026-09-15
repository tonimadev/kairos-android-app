package digital.tonima.core.ai.tools

import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.viewmodel.EventIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateEventToolTest {
    private val tool = CreateEventTool()

    @Test
    fun `name is create_event`() {
        assertEquals("create_event", tool.name)
    }

    @Test
    fun `riskLevel is CRITICAL`() {
        assertEquals(RiskLevel.CRITICAL, tool.riskLevel)
    }

    @Test
    fun `parseArguments returns CreateEvent intent for valid args`() {
        val args =
            mapOf<String, Any?>(
                "title" to "Dentista",
                "start_time" to 1000L,
                "end_time" to 2000L,
                "calendar_id" to 5L,
                "description" to "Checkup",
                "location" to "Rua A",
                "is_all_day" to false,
            )

        val result = tool.parseArguments(args)

        assertNotNull(result)
        assertTrue(result is EventIntent.CreateEvent)
        val event = result as EventIntent.CreateEvent
        assertEquals("Dentista", event.title)
        assertEquals(1000L, event.startTime)
        assertEquals(2000L, event.endTime)
        assertEquals(5L, event.calendarId)
        assertEquals("Checkup", event.description)
        assertEquals("Rua A", event.location)
        assertEquals(false, event.isAllDay)
    }

    @Test
    fun `parseArguments uses defaults for optional fields`() {
        val args =
            mapOf<String, Any?>(
                "title" to "Meeting",
                "start_time" to 100L,
                "end_time" to 200L,
            )

        val result = tool.parseArguments(args) as EventIntent.CreateEvent

        assertEquals(1L, result.calendarId)
        assertNull(result.description)
        assertNull(result.location)
        assertEquals(false, result.isAllDay)
    }

    @Test
    fun `parseArguments returns null when title is missing`() {
        val args =
            mapOf<String, Any?>(
                "start_time" to 100L,
                "end_time" to 200L,
            )

        assertNull(tool.parseArguments(args))
    }

    @Test
    fun `parseArguments returns null when title is blank`() {
        val args =
            mapOf<String, Any?>(
                "title" to "  ",
                "start_time" to 100L,
                "end_time" to 200L,
            )

        assertNull(tool.parseArguments(args))
    }

    @Test
    fun `parseArguments returns null when start_time is missing`() {
        val args =
            mapOf<String, Any?>(
                "title" to "Event",
                "end_time" to 200L,
            )

        assertNull(tool.parseArguments(args))
    }

    @Test
    fun `parseArguments returns null when end_time is missing`() {
        val args =
            mapOf<String, Any?>(
                "title" to "Event",
                "start_time" to 100L,
            )

        assertNull(tool.parseArguments(args))
    }

    @Test
    fun `parseArguments returns null when end_time is before start_time`() {
        val args =
            mapOf<String, Any?>(
                "title" to "Event",
                "start_time" to 200L,
                "end_time" to 100L,
            )

        assertNull(tool.parseArguments(args))
    }

    @Test
    fun `parseArguments returns null when end_time equals start_time`() {
        val args =
            mapOf<String, Any?>(
                "title" to "Event",
                "start_time" to 100L,
                "end_time" to 100L,
            )

        assertNull(tool.parseArguments(args))
    }
}
