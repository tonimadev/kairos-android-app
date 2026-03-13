package digital.tonima.core.permissions

interface PermissionManager {
    val calendarPermissions: List<String>
    val notificationPermissions: List<String>
    val exactAlarmPermissions: List<String>
    val fullScreenIntentPermissions: List<String>
    val locationPermissions: List<String>
    val backgroundLocationPermission: String?

    fun hasCalendarPermission(): Boolean

    fun hasExactAlarmPermission(): Boolean

    fun hasFullScreenIntentPermission(): Boolean

    fun hasPostNotificationsPermission(): Boolean

    fun hasLocationPermission(): Boolean

    fun hasBackgroundLocationPermission(): Boolean

    fun needsExactAlarmPermissionRequest(): Boolean

    fun needsFullScreenIntentPermissionRequest(): Boolean

    fun getMissingStandardPermissions(): List<String>
}
