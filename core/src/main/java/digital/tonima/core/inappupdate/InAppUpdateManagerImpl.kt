package digital.tonima.core.inappupdate

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject

@BindType(installIn = BindType.Component.ACTIVITY, to = InAppUpdateManager::class)
class InAppUpdateManagerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) :
    InAppUpdateManager {
        private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

        override fun checkForUpdate(
            updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
            updateType: Int,
            onUpdateAvailable: (() -> Unit)?,
            onNoUpdateAvailable: (() -> Unit)?,
            onUpdateInProgress: (() -> Unit)?,
            onError: ((Exception) -> Unit)?,
        ) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                when (appUpdateInfo.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        logcat {
                            "Update available. Checking type allowed."
                        }
                        if (appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                            onUpdateAvailable?.invoke()
                            startUpdateFlow(appUpdateInfo, updateType, updateLauncher)
                        } else {
                            logcat(LogPriority.WARN) {
                                "Update available, but type $updateType not allowed for this update."
                            }
                            onNoUpdateAvailable?.invoke()
                        }
                    }

                    UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                        logcat {
                            "No update available."
                        }
                        onNoUpdateAvailable?.invoke()
                    }

                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        logcat {
                            "Developer-triggered update already in progress."
                        }
                        onUpdateInProgress?.invoke()
                        if (updateType == AppUpdateType.IMMEDIATE && appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                            startUpdateFlow(appUpdateInfo, updateType, updateLauncher)
                        }
                    }

                    else -> {
                        logcat {
                            "Unknown update availability: ${appUpdateInfo.updateAvailability()}"
                        }
                    }
                }
            }.addOnFailureListener { e ->
                logcat {
                    "Failed to check for update: ${e.message}"
                }
                onError?.invoke(e)
            }
        }

        private fun startUpdateFlow(
            appUpdateInfo: AppUpdateInfo,
            updateType: Int,
            updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
        ) {
            val appUpdateOptions =
                AppUpdateOptions.newBuilder(updateType)
                    .setAllowAssetPackDeletion(false)
                    .build()

            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                updateLauncher,
                appUpdateOptions,
            )
        }

        override fun completeFlexibleUpdate() {
            appUpdateManager.completeUpdate().addOnSuccessListener {
                logcat {
                    "App update flow completed successfully."
                }
            }.addOnFailureListener { e ->
                logcat {
                    "Failed to complete flexible update: ${e.message}"
                }
            }
        }

        override fun handleImmediateUpdateOnResume(updateLauncher: ActivityResultLauncher<IntentSenderRequest>) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    logcat {
                        "Resuming immediate update."
                    }
                    startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE, updateLauncher)
                }
            }
        }
    }
