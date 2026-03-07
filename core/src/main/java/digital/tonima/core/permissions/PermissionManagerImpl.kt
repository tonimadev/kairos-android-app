package digital.tonima.core.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = PermissionManager::class)
class PermissionManagerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PermissionManager {
        override val calendarPermissions: List<String> = listOf(Manifest.permission.READ_CALENDAR)
        override val notificationPermissions: List<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyList()
            }
        override val exactAlarmPermissions: List<String> =
            emptyList()
        override val fullScreenIntentPermissions: List<String> =
            emptyList()

        override fun hasCalendarPermission(): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR,
            ) == PackageManager.PERMISSION_GRANTED
        }

        override fun hasPostNotificationsPermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        override fun hasExactAlarmPermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        }

        override fun hasFullScreenIntentPermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.canUseFullScreenIntent()
            } else {
                true
            }
        }

        override fun needsExactAlarmPermissionRequest(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission()
        }

        override fun needsFullScreenIntentPermissionRequest(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !hasFullScreenIntentPermission()
        }

        override fun getMissingStandardPermissions(): List<String> {
            val missing = mutableListOf<String>()
            if (!hasCalendarPermission()) {
                missing.add(Manifest.permission.READ_CALENDAR)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationsPermission()) {
                missing.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return missing
        }
    }
