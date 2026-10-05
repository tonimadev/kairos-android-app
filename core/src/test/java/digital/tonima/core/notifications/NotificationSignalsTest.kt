package digital.tonima.core.notifications

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationSignalsTest {
    private val hour = 60 * 60 * 1000L

    @Before
    fun setUp() = NotificationSignals.resetForTest()

    @After
    fun tearDown() = NotificationSignals.resetForTest()

    @Test
    fun `fired alarms are remembered for a while and then forgotten`() {
        NotificationSignals.alarmFired("Dentista", startTime = 10, now = 0)

        assertEquals(listOf("Dentista"), NotificationSignals.recentAlarms(now = hour).map { it.title })
        assertTrue(NotificationSignals.recentAlarms(now = 4 * hour).isEmpty())
    }

    @Test
    fun `a fired alarm is published to listeners`() =
        runTest {
            NotificationSignals.alarmFired.test {
                NotificationSignals.alarmFired("Dentista", startTime = 10, now = 5)

                assertEquals(FiredAlarm("Dentista", 10, 5), awaitItem())
            }
        }

    @Test
    fun `focus is active only inside the meeting window`() {
        NotificationSignals.focusScheduled(startTime = 1_000, endTime = 5_000)

        assertEquals(0L, NotificationSignals.activeFocusEnd(now = 999))
        assertEquals(5_000L, NotificationSignals.activeFocusEnd(now = 1_000))
        assertEquals(5_000L, NotificationSignals.activeFocusEnd(now = 4_999))
        assertEquals(0L, NotificationSignals.activeFocusEnd(now = 5_000))
    }

    @Test
    fun `all day or open ended events never start a focus window`() {
        NotificationSignals.focusScheduled(startTime = 1_000, endTime = 1_000 + 24 * hour)
        NotificationSignals.focusScheduled(startTime = 1_000, endTime = -1)

        assertEquals(0L, NotificationSignals.activeFocusEnd(now = 2_000))
    }

    @Test
    fun `ending focus clears a finished window but keeps a back to back meeting`() =
        runTest {
            NotificationSignals.focusScheduled(startTime = 1_000, endTime = 5_000)
            NotificationSignals.focusEnded(now = 5_000)
            assertEquals(0L, NotificationSignals.activeFocusEnd(now = 2_000))

            NotificationSignals.focusScheduled(startTime = 5_000, endTime = 9_000)
            NotificationSignals.focusEnded(now = 4_000)
            assertEquals(9_000L, NotificationSignals.activeFocusEnd(now = 6_000))
        }
}
