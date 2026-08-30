package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.AIToolResult
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.entity.ConversationEntity
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.usecases.AskAiAgentUseCase
import digital.tonima.core.usecases.ClearChatHistoryUseCase
import digital.tonima.core.usecases.CreateConversationUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.DeleteConversationUseCase
import digital.tonima.core.usecases.FetchMeetingTranscriptUseCase
import digital.tonima.core.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.usecases.GetChatHistoryUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.usecases.GetRegisteredAiToolsUseCase
import digital.tonima.core.usecases.InsertChatMessageUseCase
import digital.tonima.core.usecases.ObserveChatHistoryUseCase
import digital.tonima.core.usecases.ObserveConversationsUseCase
import digital.tonima.core.usecases.ObserveDailyBriefingUseCase
import digital.tonima.core.usecases.ProcessAiResponseUseCase
import digital.tonima.core.usecases.SpeakTextUseCase
import digital.tonima.core.usecases.ToggleFocusModeUseCase
import digital.tonima.core.usecases.UpdateWidgetUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class AiViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val mockProUserProvider: ProUserProvider = mockk(relaxed = true)
    private val mockAskAiAgentUseCase: AskAiAgentUseCase = mockk(relaxed = true)
    private val mockObserveDailyBriefingUseCase: ObserveDailyBriefingUseCase = mockk(relaxed = true)
    private val mockGenerateDailyBriefingUseCase: GenerateDailyBriefingUseCase = mockk(relaxed = true)
    private val mockGetRegisteredAiToolsUseCase: GetRegisteredAiToolsUseCase = mockk(relaxed = true)
    private val mockProcessAiResponseUseCase: ProcessAiResponseUseCase = mockk(relaxed = true)
    private val mockSpeakTextUseCase: SpeakTextUseCase = mockk(relaxed = true)
    private val mockUpdateWidgetUseCase: UpdateWidgetUseCase = mockk(relaxed = true)
    private val mockObserveChatHistoryUseCase: ObserveChatHistoryUseCase = mockk(relaxed = true)
    private val mockGetChatHistoryUseCase: GetChatHistoryUseCase = mockk(relaxed = true)
    private val mockInsertChatMessageUseCase: InsertChatMessageUseCase = mockk(relaxed = true)
    private val mockClearChatHistoryUseCase: ClearChatHistoryUseCase = mockk(relaxed = true)
    private val mockObserveConversationsUseCase: ObserveConversationsUseCase = mockk(relaxed = true)
    private val mockCreateConversationUseCase: CreateConversationUseCase = mockk(relaxed = true)
    private val mockDeleteConversationUseCase: DeleteConversationUseCase = mockk(relaxed = true)
    private val mockFetchMeetingTranscriptUseCase: FetchMeetingTranscriptUseCase = mockk(relaxed = true)
    private val mockGetEventsForMonthUseCase: GetEventsForMonthUseCase = mockk(relaxed = true)
    private val mockToggleFocusModeUseCase: ToggleFocusModeUseCase = mockk(relaxed = true)
    private val mockCreateEventUseCase: CreateEventUseCase = mockk(relaxed = true)
    private val mockGetAvailableCalendarsUseCase: GetAvailableCalendarsUseCase = mockk(relaxed = true)

    private val dailyBriefingFlow = MutableStateFlow<String?>(null)
    private val isProUserFlow = MutableStateFlow(false)
    private val isAiUserFlow = MutableStateFlow(false)
    private val conversationsFlow = MutableStateFlow<List<ConversationEntity>>(emptyList())

    private val fakeChatHistory = mutableListOf<ChatMessage>()
    private val fakeChatHistoryFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

    private lateinit var viewModel: AiViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeChatHistory.clear()
        fakeChatHistoryFlow.value = emptyList()

        coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns AIAgentResponse.Text("")

        every { mockProUserProvider.isProUser } returns isProUserFlow
        every { mockProUserProvider.isAiUser } returns isAiUserFlow
        every { mockObserveDailyBriefingUseCase() } returns dailyBriefingFlow
        every { mockObserveConversationsUseCase() } returns conversationsFlow

        coEvery { mockCreateConversationUseCase(any()) } returns 1L

        every { mockObserveChatHistoryUseCase(any()) } returns fakeChatHistoryFlow
        coEvery { mockGetChatHistoryUseCase(any()) } answers { fakeChatHistory.toList() }
        coEvery { mockInsertChatMessageUseCase(any(), any()) } answers {
            fakeChatHistory.add(secondArg())
            fakeChatHistoryFlow.value = fakeChatHistory.toList()
            1L
        }
        coEvery { mockClearChatHistoryUseCase(any()) } answers {
            fakeChatHistory.clear()
            fakeChatHistoryFlow.value = emptyList()
            1
        }

        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        AiViewModel(
            proUserProvider = mockProUserProvider,
            askAiAgentUseCase = mockAskAiAgentUseCase,
            observeDailyBriefingUseCase = mockObserveDailyBriefingUseCase,
            generateDailyBriefingUseCase = mockGenerateDailyBriefingUseCase,
            getRegisteredAiToolsUseCase = mockGetRegisteredAiToolsUseCase,
            processAiResponseUseCase = mockProcessAiResponseUseCase,
            speakTextUseCase = mockSpeakTextUseCase,
            updateWidgetUseCase = mockUpdateWidgetUseCase,
            observeChatHistoryUseCase = mockObserveChatHistoryUseCase,
            getChatHistoryUseCase = mockGetChatHistoryUseCase,
            insertChatMessageUseCase = mockInsertChatMessageUseCase,
            clearChatHistoryUseCase = mockClearChatHistoryUseCase,
            observeConversationsUseCase = mockObserveConversationsUseCase,
            createConversationUseCase = mockCreateConversationUseCase,
            deleteConversationUseCase = mockDeleteConversationUseCase,
            fetchMeetingTranscriptUseCase = mockFetchMeetingTranscriptUseCase,
            getEventsForMonthUseCase = mockGetEventsForMonthUseCase,
            toggleFocusModeUseCase = mockToggleFocusModeUseCase,
            createEventUseCase = mockCreateEventUseCase,
            getAvailableCalendarsUseCase = mockGetAvailableCalendarsUseCase,
        )

    @Test
    fun `generateDailyBriefing calls usecase and updates widget`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()

            coEvery { mockGenerateDailyBriefingUseCase(any(), any()) } returns "Briefing content"
            coEvery { mockUpdateWidgetUseCase.updateDailyBriefingWidget() } just Runs

            viewModel.handleIntent(AiIntent.GenerateDailyBriefing("en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            coVerify { mockGenerateDailyBriefingUseCase(any(), "en") }
            coVerify { mockUpdateWidgetUseCase.updateDailyBriefingWidget() }
            assertFalse(viewModel.uiState.value.isGeneratingBriefing)
        }

    @Test
    fun `askAi updates UI state and calls usecase`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.Text("AI Response")

            viewModel.handleIntent(AiIntent.AskAi("What's next?", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals("AI Response", state.aiResponse)
            assertFalse(state.isAskingAi)
        }

    @Test
    fun `askAi maintains chat history`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()

            val response1 = AIAgentResponse.Text("Response 1")
            val response2 = AIAgentResponse.Text("Response 2")

            coEvery {
                mockAskAiAgentUseCase(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returnsMany listOf(response1, response2)

            viewModel.handleIntent(AiIntent.AskAi("Q1", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            assertEquals("Response 1", viewModel.uiState.value.aiResponse)
            assertEquals(2, fakeChatHistory.size)

            viewModel.handleIntent(AiIntent.AskAi("Q2", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            assertEquals("Response 2", viewModel.uiState.value.aiResponse)
            assertEquals(4, fakeChatHistory.size)
            assertEquals("Q2", (fakeChatHistory[2] as ChatMessage.Text).content)
            assertEquals("Response 2", (fakeChatHistory[3] as ChatMessage.Text).content)
        }

    @Test
    fun `clearAiResponse resets UI state and clears history`() =
        runTest {
            viewModel.handleIntent(AiIntent.OpenChatDetail(1L))
            runCurrent()

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.Text("Some response")

            viewModel.handleIntent(AiIntent.AskAi("Q", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            viewModel.handleIntent(AiIntent.ClearAiResponse)
            runCurrent()

            val state = viewModel.uiState.value
            assertNull(state.aiResponse)
            assertTrue(fakeChatHistory.isEmpty())
        }

    @Test
    fun `onAIFunctionCalled with SAFE tool dispatches intent immediately`() =
        runTest {
            val safeTool =
                mockk<AITool>(relaxed = true) {
                    every { riskLevel } returns RiskLevel.SAFE
                    every { name } returns "notify_late"
                }
            val intent = AiIntent.NotifyRunningLate("test_event", "Running late!")
            coEvery {
                mockProcessAiResponseUseCase("notify_late", any())
            } returns AIToolResult.Success(safeTool, intent)

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    AIAgentResponse.FunctionCall(
                        "notify_late",
                        mapOf("eventId" to "test_event", "message" to "Running late!"),
                    ),
                    AIAgentResponse.Text("Pronto, avisei que você vai se atrasar."),
                )

            viewModel.handleIntent(AiIntent.OpenChatDetail(1L))
            runCurrent()

            viewModel.handleIntent(AiIntent.AskAi("Tell them I'm late", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            val effect = viewModel.uiState.value.effect
            assertTrue(effect is AiSideEffect.ShowSnackbar)
        }

    @Test
    fun `onAIFunctionCalled with CRITICAL tool saves pending action and emits confirmation`() =
        runTest {
            val criticalTool =
                mockk<AITool>(relaxed = true) {
                    every { riskLevel } returns RiskLevel.CRITICAL
                    every { name } returns "create_event"
                }
            val createIntent = EventIntent.CreateEvent(1L, "Meeting", null, null, 1000L, 2000L, false)
            coEvery {
                mockProcessAiResponseUseCase("create_event", any())
            } returns AIToolResult.Success(criticalTool, createIntent)

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    AIAgentResponse.FunctionCall("create_event", emptyMap()),
                    AIAgentResponse.Text("Aguardando sua confirmação para criar o evento."),
                )

            viewModel.handleIntent(AiIntent.AskAi("Create meeting", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            val effect = viewModel.uiState.value.effect
            assertTrue(effect is AiSideEffect.RequireUserConfirmation)
            assertEquals(createIntent, viewModel.uiState.value.pendingAIAction)
        }

    @Test
    fun `ApprovePendingAction executes saved intent and clears pending`() =
        runTest {
            val criticalTool =
                mockk<AITool>(relaxed = true) {
                    every { riskLevel } returns RiskLevel.CRITICAL
                    every { name } returns "create_event"
                }
            val createIntent = EventIntent.CreateEvent(1L, "Meeting", null, null, 1000L, 2000L, false)
            coEvery {
                mockProcessAiResponseUseCase("create_event", any())
            } returns AIToolResult.Success(criticalTool, createIntent)

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    AIAgentResponse.FunctionCall("create_event", emptyMap()),
                    AIAgentResponse.Text("Pronto."),
                )

            viewModel.handleIntent(AiIntent.AskAi("Create meeting", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            viewModel.handleIntent(AiIntent.ApprovePendingAction)
            runCurrent()

            assertNull(viewModel.uiState.value.pendingAIAction)
            coVerify { mockCreateEventUseCase(1L, "Meeting", any(), any(), 1000L, 2000L, false, any()) }
        }

    @Test
    fun `ToggleFocusMode intent enables DND when permission granted`() =
        runTest {
            every { mockToggleFocusModeUseCase(true) } returns Result.success(Unit)

            viewModel.handleIntent(AiIntent.ToggleFocusMode(true))
            runCurrent()

            verify { mockToggleFocusModeUseCase(true) }
        }
}
