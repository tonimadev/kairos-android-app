package digital.tonima.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WeatherExtensionsTest {
    @Test
    fun `toOpenWeatherLang maps known languages to their OpenWeather codes`() {
        assertEquals("pt_br", Locale.forLanguageTag("pt").toOpenWeatherLang())
        assertEquals("zh_cn", Locale.forLanguageTag("zh").toOpenWeatherLang())
        assertEquals("es", Locale.forLanguageTag("es").toOpenWeatherLang())
        assertEquals("fr", Locale.forLanguageTag("fr").toOpenWeatherLang())
        assertEquals("ar", Locale.forLanguageTag("ar").toOpenWeatherLang())
        assertEquals("hi", Locale.forLanguageTag("hi").toOpenWeatherLang())
        assertEquals("ja", Locale.forLanguageTag("ja").toOpenWeatherLang())
        assertEquals("ru", Locale.forLanguageTag("ru").toOpenWeatherLang())
        assertEquals("de", Locale.forLanguageTag("de").toOpenWeatherLang())
    }

    @Test
    fun `toOpenWeatherLang falls back to english for unmapped languages`() {
        assertEquals("en", Locale.forLanguageTag("en").toOpenWeatherLang())
        assertEquals("en", Locale.forLanguageTag("it").toOpenWeatherLang())
        assertEquals("en", Locale.forLanguageTag("ko").toOpenWeatherLang())
    }
}
