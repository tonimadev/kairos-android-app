package digital.tonima.kairos.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import digital.tonima.core.service.AlarmSoundAndVibrateService

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // Stop the alarm service (sound + vibration) properly via ACTION_STOP_ALARM
        AlarmSoundAndVibrateService.stopAlarm(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cancel the event notification (from AlarmNotificationReceiver)
        val notificationId = intent.getIntExtra("EXTRA_UNIQUE_ID", -1)
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        // Also cancel the foreground service notification (created by AlarmSoundAndVibrateService)
        notificationManager.cancel(AlarmSoundAndVibrateService.NOTIFICATION_ID)

        // Finish AlarmActivity if it is currently in the foreground
        context.sendBroadcast(
            Intent(AlarmSoundAndVibrateService.ACTION_FINISH_ALARM_ACTIVITY).apply {
                setPackage(context.packageName)
            },
        )
    }
}
