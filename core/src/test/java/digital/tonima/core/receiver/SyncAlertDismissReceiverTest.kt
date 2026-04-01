package digital.tonima.core.receiver

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.EntryPointAccessors
import digital.tonima.core.repository.AppStatusRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncAlertDismissReceiverTest {
    private lateinit var context: Context
    private val appStatusRepository: AppStatusRepository = mockk(relaxed = true)
    private lateinit var receiver: SyncAlertDismissReceiver

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        receiver = SyncAlertDismissReceiver()

        mockkStatic(NotificationManagerCompat::class)
        mockkStatic(EntryPointAccessors::class)

        val entryPoint = mockk<SyncAlertDismissReceiver.SyncAlertEntryPoint>()
        every { entryPoint.appStatusRepository() } returns appStatusRepository
        every {
            EntryPointAccessors.fromApplication(
                any(),
                SyncAlertDismissReceiver.SyncAlertEntryPoint::class.java,
            )
        } returns entryPoint
    }

    @After
    fun tearDown() {
        unmockkStatic(NotificationManagerCompat::class)
        unmockkStatic(EntryPointAccessors::class)
    }

    @Test
    fun `when action is DISMISS_SYNC_ALERT then mute sync alert and cancel notification`() {
        // Arrange
        val notificationId = 1002
        val intent =
            Intent(SyncAlertDismissReceiver.ACTION_DISMISS_SYNC_ALERT).apply {
                putExtra(SyncAlertDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
        val mockNotificationManager = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(any()) } returns mockNotificationManager

        // Act
        receiver.onReceive(context, intent)

        // Assert
        coVerify(timeout = 2000) { appStatusRepository.setSyncAlertMutedUntil(any()) }
        verify { mockNotificationManager.cancel(notificationId) }
    }
}
