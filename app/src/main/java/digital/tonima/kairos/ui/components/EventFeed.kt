package digital.tonima.kairos.ui.components

import digital.tonima.core.viewmodel.uimodel.EventUiModel

/** One row of the day's event list: an event card, or a slot where a native ad is shown. */
internal sealed interface EventFeedItem {
    data class Event(val event: EventUiModel) : EventFeedItem

    data class NativeAd(val index: Int) : EventFeedItem
}

internal const val MAX_NATIVE_ADS = 2
private const val FIRST_AD_AFTER_EVENTS = 3
private const val EVENTS_BETWEEN_ADS = 8

/**
 * Interleaves up to [adCount] native-ad slots into [events]. The first goes after the third event
 * (or at the end of a shorter list), the next ones every [EVENTS_BETWEEN_ADS] events, and never
 * as the very last row of a longer list, so ads stay sparse and never push the events apart.
 */
internal fun buildEventFeed(
    events: List<EventUiModel>,
    adCount: Int,
): List<EventFeedItem> {
    if (events.isEmpty() || adCount <= 0) return events.map(EventFeedItem::Event)

    val slots = mutableListOf(minOf(FIRST_AD_AFTER_EVENTS, events.size))
    while (slots.size < adCount && slots.last() + EVENTS_BETWEEN_ADS < events.size) {
        slots += slots.last() + EVENTS_BETWEEN_ADS
    }

    return buildList {
        events.forEachIndexed { position, event ->
            slots.indexOf(position).takeIf { it >= 0 }?.let { add(EventFeedItem.NativeAd(it)) }
            add(EventFeedItem.Event(event))
        }
        slots.indexOf(events.size).takeIf { it >= 0 }?.let { add(EventFeedItem.NativeAd(it)) }
    }
}
