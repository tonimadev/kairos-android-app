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
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = GetSmartNotificationSuggestionsUseCase::class)
class GetSmartNotificationSuggestionsUseCaseImpl
    @Inject
    constructor() : GetSmartNotificationSuggestionsUseCase {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        override suspend fun invoke(
            recentEvents: List<Event>,
            languageInstruction: String,
        ): String? {
            if (recentEvents.isEmpty()) return null

            val model =
                Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel("gemini-2.5-flash-lite")

            val prompt = buildSmartNotificationPrompt(recentEvents, languageInstruction)

            return try {
                val response = model.generateContent(prompt)
                response.text
            } catch (e: Exception) {
                logcat { "GetSmartNotificationSuggestionsUseCase error: ${e.message}" }
                null
            }
        }

        private fun buildSmartNotificationPrompt(
            events: List<Event>,
            languageInstruction: String,
        ): String {
            val eventsSummary =
                events.groupBy {
                    Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate().dayOfWeek
                }.map { (day, dayEvents) ->
                    val dayName = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val times =
                        dayEvents.joinToString(", ") {
                            if (it.isAllDay) {
                                "Dia todo"
                            } else {
                                timeFormatter.format(
                                    Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalTime(),
                                )
                            }
                        }
                    "$dayName: $times"
                }.joinToString("\n")

            return """
                Você é um assistente de produtividade inteligente que utiliza Machine Learning para analisar os horários do usuário.
                Com base no histórico de eventos abaixo, identifique padrões de comportamento (ex: reuniões recorrentes, dias mais ocupados, horários de pico).
                Gere UMA notificação inteligente curta e proativa que ajude o usuário a se organizar melhor.
                Exemplos:
                - "Notei que você tem muitas reuniões às segundas à tarde. Deseja silenciar notificações automaticamente nesse período?"
                - "Suas manhãs de quarta costumam ser livres. Que tal reservar esse tempo para foco profundo?"
                - "Você tem eventos seguidos amanhã entre 14h e 16h. Lembre-se de fazer uma pausa."

                Seja muito breve (máximo 2 frases).
                Use um tom amigável e útil.
                $languageInstruction

                Histórico de eventos recentes:
                $eventsSummary
                """.trimIndent()
        }
    }
