package digital.tonima.core.ai.repository

import app.cash.turbine.test
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import digital.tonima.core.ai.AIConfig
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.RiskLevel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AiModelRepositoryImplTest {
    private val rateLimiter = AiRateLimiter()
    private val repository = AiModelRepositoryImpl(rateLimiter)

    private val searchTool =
        mockk<AITool>(relaxed = true) {
            every { name } returns "search"
            every { riskLevel } returns RiskLevel.SAFE
            every { parametersSchema } returns mapOf("type" to "object", "properties" to emptyMap<String, Any>())
        }

    @Before
    fun setup() {
        mockkStatic(FirebaseApp::class)
        every { FirebaseApp.getInstance() } returns mockk(relaxed = true)
        mockkStatic("com.google.firebase.ai.FirebaseAIKt")
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseApp::class)
        unmockkStatic("com.google.firebase.ai.FirebaseAIKt")
    }

    private fun mockChatModel(chat: Chat) {
        val model =
            mockk<GenerativeModel> {
                every { startChat(any()) } returns chat
            }
        every { Firebase.ai(any(), any()) } returns
            mockk {
                every { generativeModel(any(), any(), any(), any(), any(), any(), any()) } returns model
            }
    }

    @OptIn(PublicPreviewAPI::class)
    private fun mockBriefingModel(model: GenerativeModel) {
        every { Firebase.ai(any(), any()) } returns
            mockk {
                every {
                    generativeModel(any(), any(), any(), any(), any(), any(), any(), any())
                } returns model
            }
    }

    private fun textChunk(content: String): GenerateContentResponse =
        mockk {
            every { text } returns content
            every { functionCalls } returns emptyList()
        }

    private fun functionCallChunk(
        name: String,
        args: Map<String, Any>,
    ): GenerateContentResponse =
        mockk {
            every { text } returns null
            val jsonArgs = args.mapValues { JsonPrimitive(it.value.toString()) }
            every { functionCalls } returns listOf(FunctionCallPart(name, jsonArgs))
        }

    @Test
    fun `accumulates streamed text chunks cumulatively`() =
        runTest {
            val chat =
                mockk<Chat> {
                    every { sendMessageStream(any<Content>()) } returns flowOf(textChunk("Hello"), textChunk(" world"))
                }
            mockChatModel(chat)

            repository.streamAgentResponse("system", emptySet(), emptyList(), "hi").test {
                assertEquals(AiModelResult.Text("Hello"), awaitItem())
                assertEquals(AiModelResult.Text("Hello world"), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `emits FunctionCall when the model invokes a tool`() =
        runTest {
            val chat =
                mockk<Chat> {
                    every { sendMessageStream(any<Content>()) } returns
                        flowOf(functionCallChunk("search", mapOf("query" to "lunch")))
                }
            mockChatModel(chat)

            repository.streamAgentResponse("system", setOf(searchTool), emptyList(), "find lunch").test {
                val item = awaitItem()
                assertTrue(item is AiModelResult.FunctionCall)
                assertEquals("search", (item as AiModelResult.FunctionCall).name)
                awaitComplete()
            }
        }

    @Test
    fun `classifies QuotaExceededException as RATE_LIMITED and does not retry`() =
        runTest {
            val chat =
                mockk<Chat> {
                    every { sendMessageStream(any<Content>()) } returns
                        flow { throw mockk<QuotaExceededException>() }
                }
            mockChatModel(chat)

            repository.streamAgentResponse("system", emptySet(), emptyList(), "hi").test {
                val item = awaitItem()
                assertTrue(item is AiModelResult.Error)
                assertEquals(AiErrorType.RATE_LIMITED, (item as AiModelResult.Error).type)
                awaitComplete()
            }
        }

    @Test
    fun `retries once on a NETWORK error before any chunk was emitted, then succeeds`() =
        runTest {
            var attempts = 0
            val chat =
                mockk<Chat> {
                    every { sendMessageStream(any<Content>()) } answers {
                        attempts++
                        if (attempts == 1) {
                            flow { throw mockk<RequestTimeoutException>() }
                        } else {
                            flowOf(textChunk("recovered"))
                        }
                    }
                }
            mockChatModel(chat)

            repository.streamAgentResponse("system", emptySet(), emptyList(), "hi").test {
                assertEquals(AiModelResult.Text("recovered"), awaitItem())
                awaitComplete()
            }
            assertEquals(2, attempts)
        }

    @Test
    fun `rejects the call immediately once the client-side rate limit is exhausted`() =
        runTest {
            repeat(AIConfig.MAX_REQUESTS_PER_MINUTE) {
                rateLimiter.tryAcquire()
            }

            repository.streamAgentResponse("system", emptySet(), emptyList(), "hi").test {
                val item = awaitItem()
                assertTrue(item is AiModelResult.Error)
                assertEquals(AiErrorType.RATE_LIMITED, (item as AiModelResult.Error).type)
                awaitComplete()
            }
        }

    @Test
    fun `generateBriefingContent returns the model text on success`() =
        runTest {
            val model =
                mockk<GenerativeModel> {
                    coEvery { generateContent(any<String>()) } returns textChunk("Briefing")
                }
            mockBriefingModel(model)

            val result = repository.generateBriefingContent("prompt")

            assertEquals(AiModelResult.Text("Briefing"), result)
        }

    @Test
    fun `generateBriefingContent retries a NETWORK failure and then succeeds`() =
        runTest {
            var attempts = 0
            val model =
                mockk<GenerativeModel> {
                    coEvery { generateContent(any<String>()) } answers {
                        attempts++
                        if (attempts == 1) throw mockk<RequestTimeoutException>() else textChunk("Recovered")
                    }
                }
            mockBriefingModel(model)

            val result = repository.generateBriefingContent("prompt")

            assertEquals(AiModelResult.Text("Recovered"), result)
            assertEquals(2, attempts)
        }

    @Test
    fun `generateBriefingContent is rejected immediately once rate limited`() =
        runTest {
            repeat(AIConfig.MAX_REQUESTS_PER_MINUTE) { rateLimiter.tryAcquire() }

            val result = repository.generateBriefingContent("prompt")

            assertTrue(result is AiModelResult.Error)
            assertEquals(AiErrorType.RATE_LIMITED, (result as AiModelResult.Error).type)
        }
}
