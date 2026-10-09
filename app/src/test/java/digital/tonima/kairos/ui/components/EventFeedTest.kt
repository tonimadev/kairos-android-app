package digital.tonima.kairos.ui.components

import digital.tonima.core.viewmodel.uimodel.EventUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class EventFeedTest {
    private fun events(count: Int) =
        List(count) { EventUiModel(id = it.toLong(), title = "E$it", startTime = it * 1000L) }

    private fun layout(
        eventCount: Int,
        adCount: Int,
    ) = buildEventFeed(events(eventCount), adCount).joinToString("") {
        when (it) {
            is EventFeedItem.Event -> "e"
            is EventFeedItem.NativeAd -> "A"
        }
    }

    @Test
    fun `without ads the feed is just the events in order`() {
        val feed = buildEventFeed(events(3), adCount = 0)

        assertEquals(events(3).map(EventFeedItem::Event), feed)
    }

    @Test
    fun `an empty day never shows an ad`() {
        assertEquals("", layout(eventCount = 0, adCount = 2))
    }

    @Test
    fun `a short day gets its ad after the last event`() {
        assertEquals("eA", layout(eventCount = 1, adCount = 1))
        assertEquals("eeeA", layout(eventCount = 3, adCount = 1))
    }

    @Test
    fun `the first ad goes after the third event of a longer day`() {
        assertEquals("eeeAeee", layout(eventCount = 6, adCount = 1))
    }

    @Test
    fun `a second ad needs eight more events and is never the last row`() {
        assertEquals("eeeAeeeeeeee", layout(eventCount = 11, adCount = 2))
        assertEquals("eeeAeeeeeeeeAe", layout(eventCount = 12, adCount = 2))
    }

    @Test
    fun `never more ads than were loaded`() {
        assertEquals(1, layout(eventCount = 30, adCount = 1).count { it == 'A' })
    }

    @Test
    fun `ad slots keep their index so each shows its own ad`() {
        val ads = buildEventFeed(events(12), adCount = 2).filterIsInstance<EventFeedItem.NativeAd>()

        assertEquals(listOf(0, 1), ads.map { it.index })
    }
}
