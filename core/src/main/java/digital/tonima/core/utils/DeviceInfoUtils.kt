package digital.tonima.core.utils

import android.content.Context
import android.os.Build
import android.os.UserManager

/**
 * Utility class to detect device and profile information.
 * Particularly useful for identifying Work Profile constraints.
 */
object DeviceInfoUtils {

    /**
     * Checks if the app is running in a Work Profile (Managed Profile).
     *
     * Work Profiles have more aggressive background restrictions:
     * - Doze mode is applied more strictly
     * - AlarmManager may be throttled
     * - WorkManager jobs are delayed more aggressively
     * - Profile can be paused (all alarms cancelled)
     *
     * @return true if running in a Work Profile, false otherwise
     */
    fun isWorkProfile(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                userManager?.isManagedProfile ?: false
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Gets a human-readable description of the profile type.
     * Useful for logging and debugging.
     */
    fun getProfileDescription(context: Context): String {
        return if (isWorkProfile(context)) {
            "Work Profile (Managed)"
        } else {
            "Personal Profile"
        }
    }
}

