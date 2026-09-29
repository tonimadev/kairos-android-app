package digital.tonima.core.service

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmAnnouncerTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        ShadowTextToSpeech.reset()
    }

    @Test
    fun `announcement is spoken in the app language instead of the system voice`() {
        ShadowTextToSpeech.addLanguageAvailability(PORTUGUESE)

        AlarmAnnouncer(context, "Lembrete: Reunião", PORTUGUESE) {}
        val tts = initializedTts()

        assertEquals(PORTUGUESE, tts.currentLanguage)
        assertEquals("Lembrete: Reunião", tts.lastSpokenText)
    }

    @Test
    fun `announcement is still spoken with the default voice when the app language is not installed`() {
        AlarmAnnouncer(context, "Lembrete: Reunião", PORTUGUESE) {}
        val tts = initializedTts()

        assertEquals("Lembrete: Reunião", tts.lastSpokenText)
    }

    private fun initializedTts(): ShadowTextToSpeech {
        val tts = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        tts.onInitListener.onInit(TextToSpeech.SUCCESS)
        return tts
    }

    private companion object {
        val PORTUGUESE: Locale = Locale.forLanguageTag("pt-BR")
    }
}
