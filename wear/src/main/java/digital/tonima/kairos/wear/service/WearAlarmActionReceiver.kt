package digital.tonima.kairos.wear.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import digital.tonima.core.service.AlarmSoundAndVibrateService

class WearAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmSoundAndVibrateService.stopAlarm(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra("EXTRA_UNIQUE_ID", -1)
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }
    }
}
