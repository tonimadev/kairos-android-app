package digital.tonima.core.database.mapper

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.entity.ChatHistoryEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

fun ChatHistoryEntity.toChatMessage(): ChatMessage? {
    val messageRole = if (role == "USER") ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT
    return when (type) {
        "TEXT" -> content?.let { ChatMessage.Text(messageRole, it) }
        "FUNCTION_CALL" -> {
            val name = functionName ?: return null
            ChatMessage.FunctionCall(name, parseJsonMap(functionArgsOrResponse))
        }
        "FUNCTION_RESPONSE" -> {
            val name = functionName ?: return null
            ChatMessage.FunctionResponse(name, parseJsonMap(functionArgsOrResponse))
        }
        else -> null
    }
}

fun ChatMessage.toEntity(conversationId: Long): ChatHistoryEntity {
    val roleStr = if (role == ChatMessage.Role.USER) "USER" else "ASSISTANT"
    return when (this) {
        is ChatMessage.Text ->
            ChatHistoryEntity(
                conversationId = conversationId,
                role = roleStr,
                type = "TEXT",
                content = content,
            )
        is ChatMessage.FunctionCall ->
            ChatHistoryEntity(
                conversationId = conversationId,
                role = roleStr,
                type = "FUNCTION_CALL",
                functionName = name,
                functionArgsOrResponse = args.toJsonObject().toString(),
            )
        is ChatMessage.FunctionResponse ->
            ChatHistoryEntity(
                conversationId = conversationId,
                role = roleStr,
                type = "FUNCTION_RESPONSE",
                functionName = name,
                functionArgsOrResponse = response.toJsonObject().toString(),
            )
    }
}

/**
 * Reads back what [toJsonObject] wrote. Numbers and booleans come back as their string form;
 * JSON null comes back as null; nested values as their JSON text. Corrupted JSON yields an empty map.
 */
private fun parseJsonMap(json: String?): Map<String, Any?> =
    try {
        Json.parseToJsonElement(json ?: "{}").jsonObject.mapValues { (_, element) ->
            when (element) {
                is JsonNull -> null
                is JsonPrimitive -> element.content
                else -> element.toString()
            }
        }
    } catch (_: Exception) {
        emptyMap()
    }

private fun Map<String, Any?>.toJsonObject(): JsonObject {
    return JsonObject(
        this.mapValues { (_, v) ->
            when (v) {
                is String -> JsonPrimitive(v)
                is Number -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                null -> JsonNull
                else -> JsonPrimitive(v.toString())
            }
        },
    )
}
