package digital.tonima.core.service

import digital.tonima.kairos.core.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmSchedulingWorkerTest {
    private fun event(
        id: Long,
        startTime: Long,
    ) = Event(id = id, title = "Event $id", startTime = startTime)

    @Test
    fun `selectEventForHealthCheck picks the earliest event even when it is not first in the list`() {
        val earliest = event(id = 1L, startTime = 3_000L)
        val middle = event(id = 2L, startTime = 1_000L)
        val latest = event(id = 3L, startTime = 5_000L)

        // Deliberately not sorted: the earliest event is last in the list.
        val events = listOf(earliest, latest, middle)

        val result = AlarmSchedulingWorker.selectEventForHealthCheck(events)

        assertEquals(middle, result)
    }

    @Test
    fun `selectEventForHealthCheck returns null for an empty list`() {
        val result = AlarmSchedulingWorker.selectEventForHealthCheck(emptyList())

        assertNull(result)
    }
}
