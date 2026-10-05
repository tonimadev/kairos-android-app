package digital.tonima.core.repository

import kotlinx.coroutines.flow.Flow

/** Opt-in switches for the features that need notification access. All default to off. */
interface NotificationPreferencesRepository {
    /** Opt-in (needs notification access): dismiss the calendar app's reminder once Kairos rings for the same event. */
    fun isNotificationDedupEnabled(): Flow<Boolean>

    suspend fun setNotificationDedupEnabled(enabled: Boolean)

    /** Opt-in (needs notification access): hold back chat/social notifications during meetings and summarize them. */
    fun isFocusDigestEnabled(): Flow<Boolean>

    suspend fun setFocusDigestEnabled(enabled: Boolean)

    /** Opt-in (needs notification access): suggest creating an event from invites found in notifications. */
    fun isEventSuggestionsEnabled(): Flow<Boolean>

    suspend fun setEventSuggestionsEnabled(enabled: Boolean)
}
