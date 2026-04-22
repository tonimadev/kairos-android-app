package digital.tonima.core.repository

import android.app.NotificationManager
import android.content.Context
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface FocusModeRepository {
    fun setDoNotDisturb(enabled: Boolean)

    fun isDoNotDisturbEnabled(): Boolean

    fun hasNotificationPolicyAccess(): Boolean
}

@BindType(installIn = BindType.Component.SINGLETON, to = FocusModeRepository::class)
@Singleton
class FocusModeRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FocusModeRepository {
        private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        override fun setDoNotDisturb(enabled: Boolean) {
            if (!hasNotificationPolicyAccess()) return

            val filter =
                if (enabled) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
            notificationManager.setInterruptionFilter(filter)
        }

        override fun isDoNotDisturbEnabled(): Boolean {
            return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        }

        override fun hasNotificationPolicyAccess(): Boolean {
            return notificationManager.isNotificationPolicyAccessGranted
        }
    }
