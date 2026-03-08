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

@BindType(installIn = BindType.Component.SINGLETON, to = AskAiAboutScheduleUseCase::class)
class AskAiAboutScheduleUseCaseImpl
    @Inject
    constructor() : AskAiAboutScheduleUseCase {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        override suspend fun invoke(
            events: List<Event>,
            question: String,
            languageInstruction: String,
        ): String? {
            val model =
                Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel("gemini-2.5-flash-lite")

            val prompt = buildPrompt(events, question, languageInstruction)

            return try {
                val response = model.generateContent(prompt)
                response.text
            } catch (e: Exception) {
                logcat { "AskAiAboutScheduleUseCase error: ${e.message}" }
                null
            }
        }

        private fun buildPrompt(
            events: List<Event>,
            question: String,
            languageInstruction: String,
        ): String {
            val now = java.time.LocalDateTime.now()
            val nowStr = dateTimeFormatter.format(now)

            val groupedEvents =
                events.groupBy {
                    Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                }.toSortedMap()

            val eventsStr =
                groupedEvents.entries.joinToString("\n\n") { (date, dayEvents) ->
                    "Data: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}\n" +
                        dayEvents.joinToString("\n") { event ->
                            val start =
                                Instant.ofEpochMilli(event.startTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()

                            val status = if (event.isAlarmEnabled) "" else " (Alarme Desativado)"
                            val time =
                                if (event.isAllDay) {
                                    "Dia inteiro"
                                } else {
                                    start.format(DateTimeFormatter.ofPattern("HH:mm"))
                                }
                            "- $time: ${event.title}$status"
                        }
                }

            return """
                Você é um assistente pessoal inteligente integrado a um calendário.
                Você tem acesso aos eventos do usuário para um período amplo (mês atual e arredores).
                Use esse contexto para responder a perguntas sobre hoje, amanhã ou qualquer data no calendário.
                Responda à pergunta do usuário de forma direta, útil e amigável.
                Se a pergunta for sobre horários (ex: "quando dormir"), analise os compromissos do dia seguinte para dar uma sugestão fundamentada.

                Data e Hora atual: $nowStr

                Pergunta do usuário: "$question"

                Contexto do Calendário:
                $eventsStr

                $languageInstruction
                """.trimIndent()
        }
    }
