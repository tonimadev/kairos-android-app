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
            val argsStr = functionArgsOrResponse ?: "{}"
            val args =
                try {
                    val json = Json.parseToJsonElement(argsStr).jsonObject
                    val map = mutableMapOf<String, Any?>()
                    json.forEach { (key, element) ->
                        map[key] =
                            if (element is JsonPrimitive) {
                                if (element.isString) {
                                    element.content
                                } else {
                                    element.content // we leave it as string representation of number/boolean
                                }
                            } else {
                                element.toString()
                            }
                    }
                    map
                } catch (e: Exception) {
                    emptyMap()
                }
            ChatMessage.FunctionCall(name, args)
        }
        "FUNCTION_RESPONSE" -> {
            val name = functionName ?: return null
            val respStr = functionArgsOrResponse ?: "{}"
            val resp =
                try {
                    val json = Json.parseToJsonElement(respStr).jsonObject
                    val map = mutableMapOf<String, Any?>()
                    json.forEach { (key, element) ->
                        map[key] =
                            if (element is JsonPrimitive) {
                                if (element.isString) {
                                    element.content
                                } else {
                                    element.content
                                }
                            } else {
                                element.toString()
                            }
                    }
                    map
                } catch (e: Exception) {
                    emptyMap()
                }
            ChatMessage.FunctionResponse(name, resp)
        }
        else -> null
    }
}

fun ChatMessage.toEntity(): ChatHistoryEntity {
    val roleStr = if (role == ChatMessage.Role.USER) "USER" else "ASSISTANT"
    return when (this) {
        is ChatMessage.Text ->
            ChatHistoryEntity(
                role = roleStr,
                type = "TEXT",
                content = content,
            )
        is ChatMessage.FunctionCall ->
            ChatHistoryEntity(
                role = roleStr,
                type = "FUNCTION_CALL",
                functionName = name,
                functionArgsOrResponse = args.toJsonObject().toString(),
            )
        is ChatMessage.FunctionResponse ->
            ChatHistoryEntity(
                role = roleStr,
                type = "FUNCTION_RESPONSE",
                functionName = name,
                functionArgsOrResponse = response.toJsonObject().toString(),
            )
    }
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
