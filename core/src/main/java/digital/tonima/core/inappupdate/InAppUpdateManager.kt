package digital.tonima.core.inappupdate

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.install.model.AppUpdateType

interface InAppUpdateManager {
    fun checkForUpdate(
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
        updateType: Int = AppUpdateType.FLEXIBLE,
        onUpdateAvailable: (() -> Unit)? = null,
        onNoUpdateAvailable: (() -> Unit)? = null,
        onUpdateInProgress: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
    )

    fun handleImmediateUpdateOnResume(updateLauncher: ActivityResultLauncher<IntentSenderRequest>)

    fun completeFlexibleUpdate()
}
