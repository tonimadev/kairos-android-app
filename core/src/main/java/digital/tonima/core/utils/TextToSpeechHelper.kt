package digital.tonima.core.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var tts: TextToSpeech? = null
        private var isInitialized = false
        private var onDoneCallback: (() -> Unit)? = null

        fun speak(
            text: String,
            onDone: (() -> Unit)? = null,
        ) {
            this.onDoneCallback = onDone
            if (tts == null) {
                initTts {
                    speakText(text)
                }
            } else if (isInitialized) {
                speakText(text)
            }
        }

        private fun initTts(onInitSuccess: () -> Unit) {
            tts =
                TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.getDefault())
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TTS", "Language not supported")
                        } else {
                            isInitialized = true
                            setupUtteranceListener()
                            onInitSuccess()
                        }
                    } else {
                        Log.e("TTS", "Initialization failed")
                    }
                }
        }

        private fun setupUtteranceListener() {
            tts?.setOnUtteranceProgressListener(
                object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        onDoneCallback?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        onDoneCallback?.invoke()
                    }
                },
            )
        }

        private fun speakText(text: String) {
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ai_response")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ai_response")
        }

        fun stop() {
            tts?.stop()
        }

        fun shutdown() {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        }
    }
