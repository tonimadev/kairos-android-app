package digital.tonima.core.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class AudioWarningState {
    NORMAL,
    VIBRATE,
    SILENT,
    ALARM_MUTED,
}

/**
 * Repositório para observar e expor o estado do modo de som do dispositivo em tempo real.
 */
@BindType(installIn = BindType.Component.VIEW_MODEL, to = RingerModeRepository::class)
class RingerModeRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : RingerModeRepository {
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        private val _ringerMode = MutableStateFlow(getCurrentRingerMode())
        override val ringerMode: StateFlow<AudioWarningState> = _ringerMode.asStateFlow()

        private val ringerModeReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                    if (alarmVolume == 0) {
                        _ringerMode.value = AudioWarningState.ALARM_MUTED
                        return
                    }

                    val ringerMode =
                        if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                            intent.getIntExtra(
                                AudioManager.EXTRA_RINGER_MODE,
                                audioManager.ringerMode,
                            )
                        } else {
                            audioManager.ringerMode
                        }

                    _ringerMode.value =
                        when (ringerMode) {
                            AudioManager.RINGER_MODE_VIBRATE -> AudioWarningState.VIBRATE
                            AudioManager.RINGER_MODE_SILENT -> AudioWarningState.SILENT
                            else -> AudioWarningState.NORMAL
                        }
                }
            }

        /**
         * Inicia a observação das alterações do modo de som.
         */
        override fun startObserving() {
            val filter = IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction("android.media.VOLUME_CHANGED_ACTION")
            }
            context.registerReceiver(ringerModeReceiver, filter)
        }

        /**
         * Para a observação para evitar fugas de memória.
         */
        override fun stopObserving() {
            context.unregisterReceiver(ringerModeReceiver)
        }

        private fun getCurrentRingerMode(): AudioWarningState {
            val alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (alarmVolume == 0) {
                return AudioWarningState.ALARM_MUTED
            }
            return when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioWarningState.NORMAL
                AudioManager.RINGER_MODE_VIBRATE -> AudioWarningState.VIBRATE
                AudioManager.RINGER_MODE_SILENT -> AudioWarningState.SILENT
                else -> AudioWarningState.NORMAL
            }
        }
    }
