package digital.tonima.kairos.inappupdate

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import digital.tonima.core.inappupdate.InAppUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.logcat
import digital.tonima.kairos.core.R as CoreR

class InAppUpdateDelegate(
    private val activity: ComponentActivity,
    private val inAppUpdateManager: InAppUpdateManager,
    private val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope,
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity),
) {
    private val installStateUpdatedListener: InstallStateUpdatedListener =
        InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    logcat {
                        "Update downloaded. Showing Snackbar."
                    }
                    showUpdateDownloadedSnackbar()
                }

                InstallStatus.DOWNLOADING -> {
                    val bytesDownloaded = state.bytesDownloaded()
                    val totalBytesToDownload = state.totalBytesToDownload()
                    logcat {
                        "Downloading update: $bytesDownloaded / $totalBytesToDownload"
                    }
                }

                InstallStatus.INSTALLING -> {
                    logcat {
                        "Installing update..."
                    }
                }

                InstallStatus.INSTALLED -> {
                    logcat {
                        "Update installed."
                    }
                    appUpdateManager.unregisterListener(this.installStateUpdatedListener)
                }

                else -> {
                    logcat {
                        "Install status: ${state.installStatus()}"
                    }
                }
            }
        }

    private fun showUpdateDownloadedSnackbar() {
        coroutineScope.launch {
            val snackbarResult =
                snackbarHostState.showSnackbar(
                    message = activity.getString(CoreR.string.update_downloaded_message),
                    actionLabel = activity.getString(CoreR.string.restart_app),
                    duration = SnackbarDuration.Indefinite,
                )
            when (snackbarResult) {
                SnackbarResult.ActionPerformed -> {
                    logcat { "Snackbar action 'REINICIAR' clicked." }
                    inAppUpdateManager.completeFlexibleUpdate()
                }

                SnackbarResult.Dismissed -> {
                    logcat { "Snackbar dismissed (ignored)." }
                }
            }
        }
    }

    fun onCreate() {
        appUpdateManager.registerListener(installStateUpdatedListener)
        inAppUpdateManager.checkForUpdate(
            updateLauncher = updateLauncher,
            updateType = AppUpdateType.FLEXIBLE,
        ) {
            Toast.makeText(activity, CoreR.string.checking_updates, Toast.LENGTH_SHORT).show()
        }
    }

    fun onResume() {
        inAppUpdateManager.handleImmediateUpdateOnResume(updateLauncher)
    }

    fun onDestroy() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}
