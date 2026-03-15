package digital.tonima.core.usecases

import digital.tonima.core.permissions.PermissionManager
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionState(
    val hasCalendarPermission: Boolean,
    val hasPostNotificationsPermission: Boolean,
    val hasExactAlarmPermission: Boolean,
    val hasFullScreenIntentPermission: Boolean,
    val hasLocationPermission: Boolean,
    val hasBackgroundLocationPermission: Boolean,
)

@Singleton
class CheckPermissionsUseCase
    @Inject
    constructor(
        private val permissionManager: PermissionManager,
    ) {
        operator fun invoke(): PermissionState {
            return PermissionState(
                hasCalendarPermission = permissionManager.hasCalendarPermission(),
                hasPostNotificationsPermission = permissionManager.hasPostNotificationsPermission(),
                hasExactAlarmPermission = permissionManager.hasExactAlarmPermission(),
                hasFullScreenIntentPermission = permissionManager.hasFullScreenIntentPermission(),
                hasLocationPermission = permissionManager.hasLocationPermission(),
                hasBackgroundLocationPermission = permissionManager.hasBackgroundLocationPermission(),
            )
        }
    }
