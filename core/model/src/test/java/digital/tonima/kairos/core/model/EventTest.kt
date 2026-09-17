package digital.tonima.kairos.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EventTest {
    @Test
    fun `uniqueIntentId should be consistent for same id and startTime`() {
        val e1 = Event(id = 42L, title = "A", startTime = 1700000000000)
        val e2 = Event(id = 42L, title = "B", startTime = 1700000000000)

        assertEquals(e1.uniqueIntentId, e2.uniqueIntentId)
    }

    @Test
    fun `uniqueIntentId should differ for different combinations`() {
        val base = Event(id = 1L, title = "A", startTime = 1000L)
        val diffId = Event(id = 2L, title = "A", startTime = 1000L)
        val diffTime = Event(id = 1L, title = "A", startTime = 2000L)

        val baseId = base.uniqueIntentId
        assert(baseId != diffId.uniqueIntentId)
        assert(baseId != diffTime.uniqueIntentId)
    }

    @Test
    fun `durationMinutes computes minutes between start and end`() {
        val event = Event(id = 1L, title = "A", startTime = 0L, endTime = 90 * 60_000L)

        assertEquals(90, event.durationMinutes)
    }

    @Test
    fun `durationMinutes is zero when endTime is not set`() {
        val event = Event(id = 1L, title = "A", startTime = 1_000L)

        assertEquals(0, event.durationMinutes)
    }

    @Test
    fun `durationMinutes is zero when endTime is before or equal to startTime`() {
        val equalTimes = Event(id = 1L, title = "A", startTime = 5_000L, endTime = 5_000L)
        val endBeforeStart = Event(id = 1L, title = "A", startTime = 5_000L, endTime = 1_000L)

        assertEquals(0, equalTimes.durationMinutes)
        assertEquals(0, endBeforeStart.durationMinutes)
    }

    @Test
    fun `hasMeetingUrl is true only for a non-blank meetingUrl`() {
        assertEquals(false, Event(id = 1L, title = "A", startTime = 0L, meetingUrl = null).hasMeetingUrl)
        assertEquals(false, Event(id = 1L, title = "A", startTime = 0L, meetingUrl = "   ").hasMeetingUrl)
        assertEquals(
            true,
            Event(id = 1L, title = "A", startTime = 0L, meetingUrl = "https://meet.example.com/a").hasMeetingUrl,
        )
    }
}
