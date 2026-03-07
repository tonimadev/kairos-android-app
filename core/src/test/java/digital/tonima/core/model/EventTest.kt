package digital.tonima.core.model

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
}
