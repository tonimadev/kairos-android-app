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
        AlarmSoundAndVibrateService.stopAlarm(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = intent.getIntExtra("EXTRA_UNIQUE_ID", -1)
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        notificationManager.cancel(AlarmSoundAndVibrateService.NOTIFICATION_ID)

        context.sendBroadcast(
            Intent(AlarmSoundAndVibrateService.ACTION_FINISH_ALARM_ACTIVITY).apply {
                setPackage(context.packageName)
            },
        )
    }
}
