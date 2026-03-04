package digital.tonima.kairos.wear.service

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_UNIQUE_ID
import digital.tonima.kairos.core.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WearAlarmActionReceiverTest {
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication() as Application
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "test_channel"
        val channel = NotificationChannel(channelId, "Test", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        // Ensure clean state
        notificationManager.cancelAll()
        val shadowNm = shadowOf(notificationManager)
        shadowNm.allNotifications.clear()
    }

    @Test
    fun `onReceive cancels notification with provided id`() {
        val notificationId = 4321
        val channelId = "test_channel"
        val notification =
            NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(context.getString(R.string.event_alarm))
                .setContentText("Test")
                .build()

        notificationManager.notify(notificationId, notification)

        val shadowNmBefore = shadowOf(notificationManager)
        assertEquals(1, shadowNmBefore.allNotifications.size)

        val intent =
            Intent(context, WearAlarmActionReceiver::class.java).apply {
                putExtra(EXTRA_UNIQUE_ID, notificationId)
            }
        val receiver = WearAlarmActionReceiver()
        receiver.onReceive(context, intent)

        val shadowNmAfter = shadowOf(notificationManager)
        assertEquals(0, shadowNmAfter.allNotifications.size)
    }
}
