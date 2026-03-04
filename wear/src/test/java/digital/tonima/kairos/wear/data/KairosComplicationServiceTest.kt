package digital.tonima.kairos.wear.data

import androidx.wear.watchface.complications.data.ComplicationType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KairosComplicationServiceTest {
    @Test
    fun getPreviewData_returnsDataForSupportedTypes() {
        val service = Robolectric.setupService(KairosComplicationService::class.java)

        val short = service.getPreviewData(ComplicationType.SHORT_TEXT)
        val long = service.getPreviewData(ComplicationType.LONG_TEXT)
        val ranged = service.getPreviewData(ComplicationType.RANGED_VALUE)

        assertNotNull(short)
        assertNotNull(long)
        assertNotNull(ranged)
    }

    @Test
    fun getPreviewData_returnsNullForUnsupportedType() {
        val service = Robolectric.setupService(KairosComplicationService::class.java)

        val data = service.getPreviewData(ComplicationType.NO_DATA)

        assertNull(data)
    }
}
