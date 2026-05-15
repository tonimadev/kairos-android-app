package digital.tonima.core.usecases

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.ai.AIConfig
import digital.tonima.core.model.Event
import digital.tonima.core.model.Weather
import digital.tonima.core.repository.DailyBriefingRepository
import digital.tonima.core.repository.WeatherRepository
import digital.tonima.core.util.toOpenWeatherLang
import logcat.logcat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = GenerateDailyBriefingUseCase::class)
class GenerateDailyBriefingUseCaseImpl
    @Inject
    constructor(
        private val weatherRepository: WeatherRepository,
        private val dailyBriefingRepository: DailyBriefingRepository,
    ) : GenerateDailyBriefingUseCase {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        override suspend fun invoke(
            events: List<Event>,
            languageInstruction: String,
            wakeUpTime: String?,
            city: String?,
        ): String? {
            val model =
                Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(AIConfig.GEMINI_MODEL)

            val lang = java.util.Locale.getDefault().toOpenWeatherLang()
            val weather = if (city != null) weatherRepository.getWeather(city, lang = lang) else null
            val prompt = buildPrompt(events, languageInstruction, wakeUpTime, weather)

            return try {
                val response = model.generateContent(prompt)
                val text = response.text
                if (text != null) dailyBriefingRepository.saveDailyBriefing(text)
                text
            } catch (e: Exception) {
                logcat { "GenerateDailyBriefingUseCase error: ${e.message}" }
                null
            }
        }

        private fun buildPrompt(
            events: List<Event>,
            languageInstruction: String,
            wakeUpTime: String?,
            weather: Weather?,
        ): String {
            val eventsStr =
                if (events.isEmpty()) {
                    "Nenhum evento agendado para hoje."
                } else {
                    events.joinToString("\n") { event ->
                        val time =
                            if (event.isAllDay) {
                                "Dia inteiro"
                            } else {
                                val localTime =
                                    Instant.ofEpochMilli(event.startTime)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalTime()
                                timeFormatter.format(localTime)
                            }
                        "- $time: ${event.title}"
                    }
                }

            val wakeUpContext =
                if (wakeUpTime != null) {
                    "\nO usuário acordou às $wakeUpTime. Se for mais cedo que o habitual, mencione isso suavemente."
                } else {
                    ""
                }

            val weatherContext =
                if (weather != null) {
                    "\nClima em ${weather.city}: ${weather.temperature.toInt()}°C, ${weather.description}."
                } else {
                    ""
                }

            return """
                Você é um assistente pessoal inteligente.
                Resuma os eventos de hoje do calendário abaixo e dê conselhos proativos.
                Considere o clima atual para dar dicas (ex: se vai chover, sugira levar guarda-chuva ou sair mais cedo).
                Seja breve (máximo 3-4 frases).
                Identifique conflitos ou horários apertados.
                Sugira silenciar o celular se houver muitas reuniões seguidas.
                Use um tom amigável e proativo.$wakeUpContext$weatherContext
                $languageInstruction

                Eventos de hoje:
                $eventsStr
                """.trimIndent()
        }
    }
