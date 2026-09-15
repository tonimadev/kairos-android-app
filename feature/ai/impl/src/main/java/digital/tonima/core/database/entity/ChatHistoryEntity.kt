package digital.tonima.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_history",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
    indices = [androidx.room.Index("conversationId")],
)
data class ChatHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val role: String, // "USER" or "ASSISTANT"
    val type: String, // "TEXT", "FUNCTION_CALL", "FUNCTION_RESPONSE"
    val content: String? = null,
    val functionName: String? = null,
    val functionArgsOrResponse: String? = null, // JSON string
    val timestamp: Long = System.currentTimeMillis(),
)
