package digital.tonima.core.ai.usecases

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.SINGLETON
import digital.tonima.core.ai.AIConfig
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.model.ChatMessage.FunctionResponse
import digital.tonima.kairos.core.model.Event
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import logcat.logcat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@BindType(installIn = SINGLETON, to = AskAiAgentUseCase::class)
class AskAiAgentUseCaseImpl
    @Inject
    constructor() : AskAiAgentUseCase {
        // ISO-8601 (yyyy-MM-dd) on purpose: "dd/MM/yyyy" is ambiguous to an LLM that may assume
        // US ordering (MM/dd/yyyy), which was causing the model to distrust the current date
        // entirely and ask the user clarifying questions like "which year?" instead of computing
        // relative dates ("amanhã", "próxima segunda") itself.
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        override suspend fun invoke(
            events: List<Event>,
            question: String?,
            languageInstruction: String,
            availableTools: Set<AITool>,
            history: List<ChatMessage>,
        ): AIAgentResponse {
            val functionDeclarations =
                availableTools.map { tool ->
                    convertToFunctionDeclaration(tool)
                }

            val tools =
                if (functionDeclarations.isNotEmpty()) {
                    listOf(Tool.functionDeclarations(functionDeclarations))
                } else {
                    emptyList()
                }

            val systemInstructionText = buildSystemInstruction(events, languageInstruction)

            val model =
                Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(
                        modelName = AIConfig.GEMINI_MODEL,
                        tools = tools,
                        systemInstruction = content { text(systemInstructionText) },
                    )

            val historyToUse =
                if (
                    question.isNullOrBlank() &&
                    history.isNotEmpty() &&
                    history.last() is FunctionResponse
                ) {
                    history.dropLast(1)
                } else {
                    history
                }

            val chatHistoryContents =
                historyToUse.map { message ->
                    when (message) {
                        is ChatMessage.Text -> {
                            val roleName = if (message.role == ChatMessage.Role.USER) "user" else "model"
                            content(roleName) { text(message.content) }
                        }
                        is ChatMessage.FunctionCall -> {
                            content("model") {
                                part(FunctionCallPart(message.name, message.args.toJsonElementMap()))
                            }
                        }
                        is FunctionResponse -> {
                            content("user") {
                                part(FunctionResponsePart(message.name, message.response.toJsonObject()))
                            }
                        }
                    }
                }

            val chat = model.startChat(chatHistoryContents)

            return try {
                val response =
                    if (!question.isNullOrBlank()) {
                        chat.sendMessage(question)
                    } else {
                        val lastMsg = history.last()
                        if (lastMsg is FunctionResponse) {
                            chat.sendMessage(
                                content("user") {
                                    part(
                                        FunctionResponsePart(
                                            lastMsg.name,
                                            lastMsg.response.toJsonObject(),
                                        ),
                                    )
                                },
                            )
                        } else {
                            throw IllegalArgumentException(
                                "Question cannot be null unless history ends with FunctionResponse",
                            )
                        }
                    }

                val functionCall = response.functionCalls.firstOrNull()
                if (functionCall != null) {
                    logcat { "AskAiAgent: LLM invoked tool '${functionCall.name}'" }
                    val args: Map<String, Any?> =
                        functionCall.args.mapValues { (_, element) ->
                            element.toKotlinValue()
                        }
                    AIAgentResponse.FunctionCall(
                        name = functionCall.name,
                        args = args,
                    )
                } else {
                    response.text?.let { AIAgentResponse.Text(it) }
                        ?: AIAgentResponse.Empty
                }
            } catch (e: Exception) {
                logcat { "AskAiAgent error: ${e.message}" }
                AIAgentResponse.Empty
            }
        }

        /**
         * The Gemini SDK returns function-call arguments as [JsonElement]s, but every
         * [AITool.parseArguments] implementation casts them back to plain Kotlin types
         * (`as? Number`, `as? Boolean`, `.toString()`). A numeric [JsonPrimitive] stringifies
         * to unquoted digits, so blindly calling `.toString()` here used to produce a `String`
         * that then failed every `as? Number` cast downstream (parseArguments returning null
         * for tools like CreateEventTool, RescheduleEventTool, SuggestFocusBlocksTool).
         */
        private fun JsonElement.toKotlinValue(): Any? =
            when (this) {
                is JsonNull -> null
                is JsonPrimitive ->
                    if (isString) {
                        content
                    } else {
                        booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
                    }
                else -> toString()
            }

        private fun Map<String, Any?>.toJsonElementMap(): Map<String, JsonElement> {
            return this.mapValues { (_, value) ->
                when (value) {
                    is String -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    null -> JsonNull
                    else -> JsonPrimitive(value.toString())
                }
            }
        }

        private fun Map<String, Any?>.toJsonObject(): JsonObject {
            return JsonObject(this.toJsonElementMap())
        }

        // ── Tool schema conversion ──────────────────────────────────────────

        private fun convertToFunctionDeclaration(tool: AITool): FunctionDeclaration {
            val parametersSchema = tool.parametersSchema

            @Suppress("UNCHECKED_CAST")
            val properties =
                parametersSchema["properties"] as? Map<String, Map<String, Any>> ?: emptyMap()

            @Suppress("UNCHECKED_CAST")
            val required =
                parametersSchema["required"] as? List<String> ?: emptyList()
            val allKeys = properties.keys.toList()
            val optional = allKeys.filter { it !in required }

            val schemaMap: Map<String, Schema> =
                properties.mapValues { (_, propDef) ->
                    val type = propDef["type"] as? String ?: "string"
                    val desc = propDef["description"] as? String
                    when (type) {
                        "string" -> Schema.string(description = desc)
                        "number" -> Schema.double(description = desc)
                        "integer" -> Schema.integer(description = desc)
                        "boolean" -> Schema.boolean(description = desc)
                        else -> Schema.string(description = desc)
                    }
                }

            return FunctionDeclaration(
                name = tool.name,
                description = tool.description,
                parameters = schemaMap,
                optionalParameters = optional,
            )
        }

        // ── Prompt ──────────────────────────────────────────────────────────

        private fun buildSystemInstruction(
            events: List<Event>,
            languageInstruction: String,
        ): String {
            val zone = ZoneId.systemDefault()
            val nowZoned = java.time.ZonedDateTime.now(zone)
            val nowStr = dateTimeFormatter.format(nowZoned)
            val nowEpochMillis = nowZoned.toInstant().toEpochMilli()
            val zoneOffset = nowZoned.offset.id

            val groupedEvents =
                events.groupBy {
                    Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate()
                }.toSortedMap()

            val eventsStr =
                groupedEvents.entries.joinToString("\n\n") { (date, dayEvents) ->
                    "Data: ${date.format(isoDateFormatter)}\n" +
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
                Você tem acesso aos eventos do usuário e a ferramentas (tools/functions) que podem executar ações no app.
                Use o contexto do calendário para responder perguntas e, quando o usuário pedir para executar uma ação (criar evento, buscar, ligar/desligar alarmes), invoque a ferramenta apropriada em vez de responder com texto.
                Responda APENAS perguntas relacionadas à agenda, alarmes, reuniões, eventos, ou sobre o clima/previsão do tempo. Se o usuário perguntar qualquer outra coisa não relacionada, diga gentilmente que você só pode ajudar com calendário e clima e não responda ao assunto.
                Responda de forma direta, útil e amigável.

                Data e Hora atual: $nowStr (formato yyyy-MM-dd HH:mm, fuso horário $zoneOffset)
                Equivalente em epoch millis (UTC) deste instante: $nowEpochMillis
                Ao calcular start_time/end_time para a ferramenta create_event (ou qualquer outra
                que peça epoch millis), derive o valor você mesmo a partir da Data e Hora atual e
                do fuso horário acima — nunca pergunte ao usuário o ano, mês ou fuso horário de
                datas relativas como "amanhã", "hoje à noite" ou "próxima segunda".

                Contexto do Calendário:
                $eventsStr

                $languageInstruction
                """.trimIndent()
        }
    }
