package digital.tonima.core.service

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import logcat.LogPriority
import logcat.logcat
import java.util.Locale

/**
 * Reads the event aloud over the alarm stream. [onFinished] is called exactly once when the speech ends,
 * or right away when text-to-speech is unavailable, so the caller can bring the ringtone back to full volume.
 */
class AlarmAnnouncer(
    context: Context,
    private val text: String,
    private val locale: Locale,
    private val onFinished: () -> Unit,
) {
    private var finished = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status -> onInit(status) }

    private fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            logcat(LogPriority.WARN) { "AlarmAnnouncer: text-to-speech unavailable (status $status)" }
            finish()
            return
        }
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        useAppLanguage()
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) = finish()

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    logcat(LogPriority.WARN) { "AlarmAnnouncer: failed to speak the event" }
                    finish()
                }
            },
        )
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) != TextToSpeech.SUCCESS) {
            logcat(LogPriority.WARN) { "AlarmAnnouncer: speak request rejected" }
            finish()
        }
    }

    /** Speaks in the app language; keeps the engine's default voice when that language is not installed. */
    private fun useAppLanguage() {
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            logcat(
                LogPriority.WARN,
            ) { "AlarmAnnouncer: no voice for $locale (result $result), using the default voice" }
        }
    }

    @Synchronized
    private fun finish() {
        if (finished) return
        finished = true
        onFinished()
    }

    @Synchronized
    fun release() {
        finished = true
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val UTTERANCE_ID = "kairos_alarm_announcement"
    }
}
