package digital.tonima.core.utils

import android.content.Context
import android.os.Build
import android.os.UserManager
import logcat.LogPriority
import logcat.logcat

object DeviceInfoUtils {
    fun isWorkProfile(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                userManager?.isManagedProfile ?: false
            } catch (e: SecurityException) {
                logcat(LogPriority.WARN) { "Could not check for a work profile: ${e.message}" }
                false
            }
        } else {
            false
        }
    }

    fun getProfileDescription(context: Context): String {
        return if (isWorkProfile(context)) {
            "Work Profile (Managed)"
        } else {
            "Personal Profile"
        }
    }
}
