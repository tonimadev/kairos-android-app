package digital.tonima.core.ai.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.UnknownException
import com.google.firebase.ai.type.content
import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.ai.AIConfig
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.model.ChatMessage.FunctionResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Only place in the module that touches the Firebase AI SDK directly: builds requests from
 * domain types, streams/generates content, retries transient failures, enforces the client-side
 * rate limit and classifies exceptions into [AiErrorType] so callers never see SDK types.
 */
@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = AiModelRepository::class)
class AiModelRepositoryImpl
    @Inject
    constructor(
        private val rateLimiter: AiRateLimiter,
    ) : AiModelRepository {
        override fun streamAgentResponse(
            systemInstruction: String,
            availableTools: Set<AITool>,
            history: List<ChatMessage>,
            question: String?,
        ): Flow<AiModelResult> =
            flow {
                if (!rateLimiter.tryAcquire()) {
                    emit(AiModelResult.Error(AiErrorType.RATE_LIMITED, RateLimitedException))
                    return@flow
                }

                val tools = buildTools(availableTools)
                val model =
                    Firebase.ai(backend = GenerativeBackend.googleAI())
                        .generativeModel(
                            modelName = AIConfig.GEMINI_MODEL,
                            tools = tools,
                            systemInstruction = content { text(systemInstruction) },
                        )

                // The Gemini chat API requires any pending FunctionResponse to be sent as the new
                // turn's message, not left inside the history context — pop it back out here.
                val historyToUse =
                    if (question.isNullOrBlank() && history.isNotEmpty() && history.last() is FunctionResponse) {
                        history.dropLast(1)
                    } else {
                        history
                    }
                val chat = model.startChat(historyToUse.toContents())

                val prompt: Content =
                    if (!question.isNullOrBlank()) {
                        content("user") { text(question) }
                    } else {
                        val lastMsg = history.lastOrNull()
                        if (lastMsg is FunctionResponse) {
                            content("user") {
                                part(FunctionResponsePart(lastMsg.name, lastMsg.response.toJsonObject()))
                            }
                        } else {
                            emit(
                                AiModelResult.Error(
                                    AiErrorType.UNKNOWN,
                                    IllegalArgumentException(
                                        "Question cannot be null unless history ends with FunctionResponse",
                                    ),
                                ),
                            )
                            return@flow
                        }
                    }

                var accumulated = ""
                var emittedAny = false
                var attempt = 0
                while (true) {
                    try {
                        chat.sendMessageStream(prompt).collect { chunk ->
                            val functionCall = chunk.functionCalls.firstOrNull()
                            if (functionCall != null) {
                                logcat { "AiModelRepository: LLM invoked tool '${functionCall.name}'" }
                                val args = functionCall.args.mapValues { (_, element) -> element.toKotlinValue() }
                                emit(AiModelResult.FunctionCall(functionCall.name, args))
                                emittedAny = true
                            } else {
                                val delta = chunk.text
                                if (!delta.isNullOrEmpty()) {
                                    accumulated += delta
                                    emit(AiModelResult.Text(accumulated))
                                    emittedAny = true
                                }
                            }
                        }
                        break
                    } catch (e: Exception) {
                        val type = classifyError(e)
                        logcat { "AiModelRepository stream error ($type): ${e.message}" }
                        if (!emittedAny && type == AiErrorType.NETWORK && attempt < AIConfig.RETRY_MAX_ATTEMPTS) {
                            attempt++
                            delay(AIConfig.RETRY_BASE_DELAY_MS * attempt)
                            continue
                        }
                        emit(AiModelResult.Error(type, e))
                        break
                    }
                }
            }

        @OptIn(PublicPreviewAPI::class)
        override suspend fun generateBriefingContent(prompt: String): AiModelResult {
            if (!rateLimiter.tryAcquire()) {
                return AiModelResult.Error(AiErrorType.RATE_LIMITED, RateLimitedException)
            }

            val model =
                Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(
                        modelName = AIConfig.GEMINI_MODEL,
                        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.PREFER_ON_DEVICE),
                    )

            var attempt = 0
            while (true) {
                try {
                    val response = model.generateContent(prompt)
                    val text = response.text
                    return if (text != null) {
                        AiModelResult.Text(text)
                    } else {
                        AiModelResult.Error(AiErrorType.UNKNOWN, EmptyResponseException)
                    }
                } catch (e: Exception) {
                    val type = classifyError(e)
                    logcat { "AiModelRepository briefing error ($type): ${e.message}" }
                    if (type == AiErrorType.NETWORK && attempt < AIConfig.RETRY_MAX_ATTEMPTS) {
                        attempt++
                        delay(AIConfig.RETRY_BASE_DELAY_MS * attempt)
                        continue
                    }
                    return AiModelResult.Error(type, e)
                }
            }
        }

        private fun classifyError(e: Throwable): AiErrorType =
            when (e) {
                is RequestTimeoutException, is ServerException, is UnknownException -> AiErrorType.NETWORK
                is QuotaExceededException -> AiErrorType.RATE_LIMITED
                is PromptBlockedException,
                is ContentBlockedException,
                is ResponseStoppedException,
                -> AiErrorType.SAFETY_BLOCKED
                else -> AiErrorType.UNKNOWN
            }

        // ── Domain → SDK conversion ─────────────────────────────────────────

        private fun buildTools(availableTools: Set<AITool>): List<Tool> {
            val functionDeclarations = availableTools.map { convertToFunctionDeclaration(it) }
            return if (functionDeclarations.isNotEmpty()) {
                listOf(Tool.functionDeclarations(functionDeclarations))
            } else {
                emptyList()
            }
        }

        private fun List<ChatMessage>.toContents(): List<Content> =
            map { message ->
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

        private object RateLimitedException : Exception("Client-side rate limit exceeded")

        private object EmptyResponseException : Exception("Model returned an empty response")
    }
