package digital.tonima.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BillingManager {
    val isProUser: StateFlow<Boolean>

    /** Emits when [launchPurchaseFlow] could not reach the store in time, so the UI can tell the user. */
    val purchaseErrors: Flow<Unit>

    fun connect()

    fun launchPurchaseFlow(activity: Activity)

    fun refresh()
}
