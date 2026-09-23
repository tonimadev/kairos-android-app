package digital.tonima.core.ai.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.AppDatabase
import digital.tonima.core.database.dao.ChatHistoryDao
import digital.tonima.core.database.entity.ChatHistoryEntity
import digital.tonima.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs the real Room DAO against an in-memory database, so the SQL itself is under test. */
@RunWith(RobolectricTestRunner::class)
class ChatHistoryRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ChatHistoryDao
    private lateinit var repository: ChatHistoryRepositoryImpl

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.chatHistoryDao()
        repository = ChatHistoryRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `messages are stored per conversation and read back in order`() =
        runTest {
            val chat = repository.createConversation("Agenda")
            val other = repository.createConversation("Outra")
            val question = ChatMessage.Text(ChatMessage.Role.USER, "Qual minha próxima reunião?")
            val call = ChatMessage.FunctionCall("search_events", mapOf("query" to "reunião"))
            val answer = ChatMessage.Text(ChatMessage.Role.ASSISTANT, "Às 14h, Planejamento.")

            repository.insertMessage(chat, question)
            repository.insertMessage(chat, call)
            repository.insertMessage(chat, answer)
            repository.insertMessage(other, ChatMessage.Text(ChatMessage.Role.USER, "oi"))

            assertEquals(listOf(question, call, answer), repository.getHistory(chat))
            assertEquals(listOf(question, call, answer), repository.observeHistory(chat).first())
        }

    @Test
    fun `history is ordered by timestamp, not insertion order`() =
        runTest {
            val chat = repository.createConversation("Agenda")
            dao.insertMessage(entity(chat, "second", timestamp = 2_000L))
            dao.insertMessage(entity(chat, "first", timestamp = 1_000L))

            val texts = repository.getHistory(chat).map { (it as ChatMessage.Text).content }

            assertEquals(listOf("first", "second"), texts)
        }

    @Test
    fun `rows that cannot be displayed are skipped`() =
        runTest {
            val chat = repository.createConversation("Agenda")
            dao.insertMessage(entity(chat, "ok", timestamp = 1L))
            dao.insertMessage(
                ChatHistoryEntity(conversationId = chat, role = "ASSISTANT", type = "UNKNOWN", timestamp = 2L),
            )

            assertEquals(1, repository.getHistory(chat).size)
        }

    @Test
    fun `conversations are listed with the most recently active first`() =
        runTest {
            val old = dao.insertConversation(ConversationEntity(title = "Old", createdAt = 1L, updatedAt = 1L))
            val recent = dao.insertConversation(ConversationEntity(title = "Recent", createdAt = 2L, updatedAt = 2L))

            assertEquals(listOf(recent, old), repository.observeConversations().first().map { it.id })

            // A new message bumps the older conversation to the top.
            repository.insertMessage(old, ChatMessage.Text(ChatMessage.Role.USER, "de volta"))

            assertEquals(listOf(old, recent), repository.observeConversations().first().map { it.id })
        }

    @Test
    fun `deleting a conversation also deletes its messages`() =
        runTest {
            val chat = repository.createConversation("Agenda")
            repository.insertMessage(chat, ChatMessage.Text(ChatMessage.Role.USER, "oi"))

            repository.deleteConversation(chat)

            assertTrue(repository.observeConversations().first().isEmpty())
            assertTrue("Messages must be removed with their conversation", dao.getHistory(chat).isEmpty())
        }

    @Test
    fun `clearing a conversation keeps the conversation and the other chats`() =
        runTest {
            val chat = repository.createConversation("Agenda")
            val other = repository.createConversation("Outra")
            repository.insertMessage(chat, ChatMessage.Text(ChatMessage.Role.USER, "a"))
            repository.insertMessage(chat, ChatMessage.Text(ChatMessage.Role.USER, "b"))
            repository.insertMessage(other, ChatMessage.Text(ChatMessage.Role.USER, "c"))

            val removed = repository.clearHistory(chat)

            assertEquals(2, removed)
            assertTrue(repository.getHistory(chat).isEmpty())
            assertEquals(1, repository.getHistory(other).size)
            assertEquals(2, repository.observeConversations().first().size)
        }

    private fun entity(
        conversationId: Long,
        text: String,
        timestamp: Long,
    ) = ChatHistoryEntity(
        conversationId = conversationId,
        role = "USER",
        type = "TEXT",
        content = text,
        timestamp = timestamp,
    )
}
