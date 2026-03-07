package digital.tonima.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmOffsetTest {

    @Test
    fun `fromMinutes returns correct enum member`() {
        assertEquals(AlarmOffset.AT_TIME, AlarmOffset.fromMinutes(0))
        assertEquals(AlarmOffset.FIFTEEN_MINUTES, AlarmOffset.fromMinutes(15))
        assertEquals(AlarmOffset.THIRTY_MINUTES, AlarmOffset.fromMinutes(30))
        assertEquals(AlarmOffset.ONE_HOUR, AlarmOffset.fromMinutes(60))
    }

    @Test
    fun `fromMinutes returns AT_TIME for unknown minutes`() {
        assertEquals(AlarmOffset.AT_TIME, AlarmOffset.fromMinutes(10))
        assertEquals(AlarmOffset.AT_TIME, AlarmOffset.fromMinutes(-1))
    }

    @Test
    fun `minutes property matches enum values`() {
        assertEquals(0L, AlarmOffset.AT_TIME.minutes)
        assertEquals(15L, AlarmOffset.FIFTEEN_MINUTES.minutes)
        assertEquals(30L, AlarmOffset.THIRTY_MINUTES.minutes)
        assertEquals(60L, AlarmOffset.ONE_HOUR.minutes)
    }
}
