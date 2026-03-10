package digital.tonima.core.usecases

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.model.Event
import logcat.logcat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = GenerateDailyBriefingUseCase::class)
class GenerateDailyBriefingUseCaseImpl
    @Inject
    constructor() : GenerateDailyBriefingUseCase {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        override suspend fun invoke(
            events: List<Event>,
            languageInstruction: String,
            wakeUpTime: String?,
        ): String? {
            val model =
                Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel("gemini-2.5-flash-lite")

            val prompt = buildPrompt(events, languageInstruction, wakeUpTime)

            return try {
                val response = model.generateContent(prompt)
                response.text
            } catch (e: Exception) {
                logcat { "GenerateDailyBriefingUseCase error: ${e.message}" }
                null
            }
        }

        private fun buildPrompt(
            events: List<Event>,
            languageInstruction: String,
            wakeUpTime: String?,
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

            return """
                Você é um assistente pessoal inteligente.
                Resuma os eventos de hoje do calendário abaixo e dê conselhos proativos.
                Seja breve (máximo 3-4 frases).
                Identifique conflitos ou horários apertados.
                Sugira silenciar o celular se houver muitas reuniões seguidas.
                Use um tom amigável e proativo.$wakeUpContext
                $languageInstruction

                Eventos de hoje:
                $eventsStr
                """.trimIndent()
        }
    }
