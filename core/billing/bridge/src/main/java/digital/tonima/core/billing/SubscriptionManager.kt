package digital.tonima.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionManager {
    val isProUser: StateFlow<Boolean>

    fun connect()

    fun launchSubscriptionFlow(activity: Activity)

    fun refresh()
}
