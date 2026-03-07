package digital.tonima.core.repository

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RingerModeRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var audioManager: AudioManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication() as Application
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Test
    fun `initial state reflects NORMAL when ringer is normal and alarm volume greater than 0`() =
        runTest {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)

            val repo = RingerModeRepositoryImpl(context)
            assertEquals(AudioWarningState.NORMAL, repo.ringerMode.first())
        }

    @Test
    fun `initial state reflects VIBRATE when ringer vibrate and alarm volume greater than 0`() =
        runTest {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)

            val repo = RingerModeRepositoryImpl(context)
            assertEquals(AudioWarningState.VIBRATE, repo.ringerMode.first())
        }

    @Test
    fun `initial state reflects SILENT when ringer silent and alarm volume greater than 0`() =
        runTest {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)

            val repo = RingerModeRepositoryImpl(context)
            assertEquals(AudioWarningState.SILENT, repo.ringerMode.first())
        }

    @Test
    fun `initial state reflects ALARM_MUTED when alarm volume is 0 regardless of ringer`() =
        runTest {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL

            val repo = RingerModeRepositoryImpl(context)
            assertEquals(AudioWarningState.ALARM_MUTED, repo.ringerMode.first())
        }

    @Test
    fun `receiver updates state on RINGER_MODE_CHANGED broadcast`() =
        runTest {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            val repo = RingerModeRepositoryImpl(context)
            // Start observing to register receiver
            repo.startObserving()

            // Change to vibrate and send broadcast (include EXTRA_RINGER_MODE for reliability under Robolectric)
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            context.sendBroadcast(
                Intent(AudioManager.RINGER_MODE_CHANGED_ACTION).apply {
                    putExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_VIBRATE)
                },
            )

            // Ensure the broadcast receiver has been processed on the main looper
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertEquals(AudioWarningState.VIBRATE, repo.ringerMode.first())

            // Change to silent and send broadcast
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            context.sendBroadcast(
                Intent(AudioManager.RINGER_MODE_CHANGED_ACTION).apply {
                    putExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_SILENT)
                },
            )
            // Ensure the broadcast receiver has been processed on the main looper
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertEquals(AudioWarningState.SILENT, repo.ringerMode.first())

            repo.stopObserving()
        }
}
