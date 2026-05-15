package digital.tonima.core.util

import java.util.Locale

/**
 * Maps the current system locale to a language code supported by OpenWeather.
 */
fun Locale.toOpenWeatherLang(): String {
    return when (language) {
        "pt" -> "pt_br"
        "zh" -> "zh_cn"
        "es" -> "es"
        "fr" -> "fr"
        "ar" -> "ar"
        "hi" -> "hi"
        "ja" -> "ja"
        "ru" -> "ru"
        "de" -> "de"
        else -> "en"
    }
}
