package digital.tonima.kairos.service

import android.service.quicksettings.Tile
import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalAlarmsTileServiceTest {
    @Test
    fun `when global alarms are enabled then the tile is active`() {
        assertEquals(Tile.STATE_ACTIVE, globalAlarmsTileState(enabled = true))
    }

    @Test
    fun `when global alarms are disabled then the tile is inactive`() {
        assertEquals(Tile.STATE_INACTIVE, globalAlarmsTileState(enabled = false))
    }
}
