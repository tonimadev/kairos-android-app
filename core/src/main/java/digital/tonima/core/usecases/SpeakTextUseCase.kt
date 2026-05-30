package digital.tonima.core.usecases

import digital.tonima.core.utils.TextToSpeechHelper
import javax.inject.Inject

class SpeakTextUseCase
    @Inject
    constructor(
        private val textToSpeechHelper: TextToSpeechHelper,
    ) {
        operator fun invoke(
            text: String,
            onDone: (() -> Unit)? = null,
        ) {
            textToSpeechHelper.speak(text, onDone)
        }

        fun stop() {
            textToSpeechHelper.stop()
        }
    }
