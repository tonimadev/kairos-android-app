package digital.tonima.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionManager {
    val isProUser: StateFlow<Boolean>

    /** Emits when [launchSubscriptionFlow] could not reach the store in time, so the UI can tell the user. */
    val subscriptionErrors: Flow<Unit>

    fun connect()

    fun launchSubscriptionFlow(activity: Activity)

    fun refresh()
}
