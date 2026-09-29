package digital.tonima.core.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmVolumeRampTest {
    @Test
    fun `ramp starts quiet and reaches full volume at the end`() {
        assertEquals(ALARM_START_VOLUME, alarmVolumeAt(0L), DELTA)
        assertEquals(ALARM_FULL_VOLUME, alarmVolumeAt(ALARM_VOLUME_RAMP_MS), DELTA)
    }

    @Test
    fun `ramp is linear halfway through`() {
        val expected = (ALARM_START_VOLUME + ALARM_FULL_VOLUME) / 2

        assertEquals(expected, alarmVolumeAt(ALARM_VOLUME_RAMP_MS / 2), DELTA)
    }

    @Test
    fun `ramp never goes up again after reaching full volume`() {
        assertEquals(ALARM_FULL_VOLUME, alarmVolumeAt(ALARM_VOLUME_RAMP_MS * 10), DELTA)
    }

    @Test
    fun `ramp volume only grows over time`() {
        val volumes = (0L..ALARM_VOLUME_RAMP_MS step ALARM_VOLUME_STEP_MS).map { alarmVolumeAt(it) }

        assertTrue(volumes.zipWithNext().all { (previous, next) -> next > previous })
    }

    @Test
    fun `negative elapsed time is treated as the start of the ramp`() {
        assertEquals(ALARM_START_VOLUME, alarmVolumeAt(-500L), DELTA)
    }

    @Test
    fun `a non positive ramp duration plays at full volume`() {
        assertEquals(ALARM_FULL_VOLUME, alarmVolumeAt(0L, rampMs = 0L), DELTA)
    }

    @Test
    fun `ringtone starts quiet only when escalating volume or the announcement is enabled`() {
        assertEquals(ALARM_FULL_VOLUME, alarmInitialVolume(escalatingVolume = false, announceEvent = false), DELTA)
        assertEquals(ALARM_START_VOLUME, alarmInitialVolume(escalatingVolume = true, announceEvent = false), DELTA)
        assertEquals(ALARM_START_VOLUME, alarmInitialVolume(escalatingVolume = false, announceEvent = true), DELTA)
        assertEquals(ALARM_START_VOLUME, alarmInitialVolume(escalatingVolume = true, announceEvent = true), DELTA)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
