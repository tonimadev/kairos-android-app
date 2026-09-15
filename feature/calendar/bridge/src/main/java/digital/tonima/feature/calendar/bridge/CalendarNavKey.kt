package digital.tonima.feature.calendar.bridge

import digital.tonima.kairos.core.navigation.AppNavigator
import digital.tonima.kairos.core.navigation.FeatureNavKey

/** Destinations owned by the calendar feature, reachable through [AppNavigator]. */
sealed interface CalendarNavKey : FeatureNavKey {
    data object Main : CalendarNavKey

    data object ImportCalendar : CalendarNavKey

    data object ManageCalendars : CalendarNavKey
}
