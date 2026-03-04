package digital.tonima.kairos.wear.service

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NextEventTileServiceFormattingTest {
    @Test
    fun formatEventTimeLocalized_returnsNonEmptyString() {
        val service = Robolectric.setupService(NextEventTileService::class.java)
        val method =
            NextEventTileService::class.java.getDeclaredMethod(
                "formatEventTimeLocalized",
                android.content.Context::class.java,
                Long::class.javaPrimitiveType,
            )
        method.isAccessible = true

        val sampleEpochMillis = 1_700_000_000_000L
        val result = method.invoke(service, service, sampleEpochMillis) as String

        assertTrue(result.isNotBlank())
    }

    @Test
    fun formatCurrentTimeLocalized_returnsNonEmptyString() {
        val service = Robolectric.setupService(NextEventTileService::class.java)
        val method =
            NextEventTileService::class.java.getDeclaredMethod(
                "formatCurrentTimeLocalized",
                android.content.Context::class.java,
            )
        method.isAccessible = true

        val result = method.invoke(service, service) as String
        assertTrue(result.isNotBlank())
    }

    @Test
    fun formatCurrentDateLocalized_returnsNonEmptyString() {
        val service = Robolectric.setupService(NextEventTileService::class.java)
        val method =
            NextEventTileService::class.java.getDeclaredMethod(
                "formatCurrentDateLocalized",
                android.content.Context::class.java,
            )
        method.isAccessible = true

        val result = method.invoke(service, service) as String
        assertTrue(result.isNotBlank())
    }
}
