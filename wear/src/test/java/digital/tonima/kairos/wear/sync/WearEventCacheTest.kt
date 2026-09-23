package digital.tonima.kairos.wear.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import digital.tonima.kairos.core.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = android.app.Application::class)
class WearEventCacheTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearCache() {
        context.getSharedPreferences("PhoneEventsCache", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `load returns an empty list when nothing was cached`() {
        assertTrue(WearEventCache.load(context).isEmpty())
    }

    @Test
    fun `saved events are loaded back with every synced field`() {
        val events =
            listOf(
                Event(
                    id = 1L,
                    title = "Dentist",
                    startTime = 1_800_000_000_000L,
                    isRecurring = true,
                    isAllDay = false,
                    location = "Rua Augusta, 500",
                    departureTime = 1_799_999_000_000L,
                    travelTimeMinutes = 17,
                ),
                Event(id = 2L, title = "Holiday", startTime = 1_800_050_000_000L, isAllDay = true),
            )

        WearEventCache.save(context, events)

        assertEquals(events, WearEventCache.load(context))
    }

    @Test
    fun `optional fields stay null when they were not provided`() {
        WearEventCache.save(context, listOf(Event(id = 3L, title = "Call", startTime = 1_000L)))

        val loaded = WearEventCache.load(context).single()
        assertNull(loaded.location)
        assertNull(loaded.departureTime)
        assertNull(loaded.travelTimeMinutes)
    }

    @Test
    fun `saving replaces the previous cache instead of appending`() {
        WearEventCache.save(context, listOf(Event(id = 1L, title = "Old", startTime = 1_000L)))
        WearEventCache.save(context, listOf(Event(id = 2L, title = "New", startTime = 2_000L)))

        assertEquals(listOf(2L), WearEventCache.load(context).map { it.id })
    }

    @Test
    fun `saving an empty list clears the cache`() {
        WearEventCache.save(context, listOf(Event(id = 1L, title = "Old", startTime = 1_000L)))
        WearEventCache.save(context, emptyList())

        assertTrue(WearEventCache.load(context).isEmpty())
    }

    @Test
    fun `corrupted cache is treated as empty instead of crashing`() {
        context
            .getSharedPreferences("PhoneEventsCache", Context.MODE_PRIVATE)
            .edit()
            .putString("json", "{not json")
            .commit()

        assertTrue(WearEventCache.load(context).isEmpty())
    }

    @Test
    fun `titles with special characters survive the round trip`() {
        val title = "Reunião \"Q3\" — café ☕ / 50%"
        WearEventCache.save(context, listOf(Event(id = 1L, title = title, startTime = 1_000L)))

        assertEquals(title, WearEventCache.load(context).single().title)
    }
}
