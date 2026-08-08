package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

    private lateinit var viewModel: AiViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { mockProUserProvider.isProUser } returns isProUserFlow
        every { mockProUserProvider.isAiUser } returns isAiUserFlow
        every { mockObserveDailyBriefingUseCase() } returns dailyBriefingFlow
        every { mockObserveConversationsUseCase() } returns conversationsFlow

        coEvery { mockCreateConversationUseCase(any()) } returns 1L
        every { mockObserveChatHistoryUseCase(any()) } answers { MutableStateFlow(fakeChatHistory.toList()) }
        coEvery { mockGetChatHistoryUseCase(any()) } answers { fakeChatHistory.toList() }
        coEvery { mockInsertChatMessageUseCase(any(), any()) } answers {
            fakeChatHistory.add(secondArg())
            1L
        }
        coEvery { mockClearChatHistoryUseCase(any()) } answers {
            fakeChatHistory.clear()
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
            advanceUntilIdle()

            coEvery { mockGenerateDailyBriefingUseCase(any(), any()) } returns "Briefing content"
            coEvery { mockUpdateWidgetUseCase.updateDailyBriefingWidget() } just Runs

            viewModel.handleIntent(AiIntent.GenerateDailyBriefing("en"))
            advanceUntilIdle()

            coVerify { mockGenerateDailyBriefingUseCase(any(), "en") }
            coVerify { mockUpdateWidgetUseCase.updateDailyBriefingWidget() }
            assertFalse(viewModel.uiState.value.isGeneratingBriefing)
        }

    @Test
    fun `askAi updates UI state and calls usecase`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.Text("AI Response")

            viewModel.handleIntent(AiIntent.AskAi("What's next?", "en"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("AI Response", state.aiResponse)
            assertFalse(state.isAskingAi)
        }

    @Test
    fun `askAi maintains chat history`() =
        runTest {
            isAiUserFlow.value = true
            advanceUntilIdle()

            val response1 = AIAgentResponse.Text("Response 1")
            val response2 = AIAgentResponse.Text("Response 2")

            coEvery { mockAskAiAgentUseCase(any(), "Q1", any(), any(), any()) } returns response1
            coEvery { mockAskAiAgentUseCase(any(), "Q2", any(), any(), any()) } returns response2

            // First interaction
            viewModel.handleIntent(AiIntent.AskAi("Q1", "en"))
            advanceUntilIdle()

            assertEquals("Response 1", viewModel.uiState.value.aiResponse)
            assertEquals(2, fakeChatHistory.size)

            // Second interaction
            viewModel.handleIntent(AiIntent.AskAi("Q2", "en"))
            advanceUntilIdle()

            assertEquals("Response 2", viewModel.uiState.value.aiResponse)
            assertEquals(4, fakeChatHistory.size)
            assertEquals("Q2", (fakeChatHistory[2] as ChatMessage.Text).content)
            assertEquals("Response 2", (fakeChatHistory[3] as ChatMessage.Text).content)
        }

    @Test
    fun `clearAiResponse resets UI state and clears history`() =
        runTest {
            viewModel.handleIntent(AiIntent.OpenChatDetail(1L))
            advanceUntilIdle()

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.Text("Some response")
            viewModel.handleIntent(AiIntent.AskAi("Q", "en"))
            advanceUntilIdle()

            viewModel.handleIntent(AiIntent.ClearAiResponse)
            advanceUntilIdle()

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
                mockProcessAiResponseUseCase(
                    "notify_late",
                    any(),
                )
            } returns AIToolResult.Success(safeTool, intent)

            viewModel.handleIntent(AiIntent.OpenChatDetail(1L))
            advanceUntilIdle()

            // We need to trigger a function call. Let's mock askAiAgent to return a FunctionCall.
            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.FunctionCall(
                    "notify_late",
                    mapOf("eventId" to "test_event", "message" to "Running late!"),
                )

            viewModel.sideEffect.test {
                viewModel.handleIntent(AiIntent.AskAi("Tell them I'm late", "en"))
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is AiSideEffect.ShowSnackbar)
                cancelAndConsumeRemainingEvents()
            }
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
                mockProcessAiResponseUseCase(
                    "create_event",
                    any(),
                )
            } returns AIToolResult.Success(criticalTool, createIntent)

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.FunctionCall("create_event", emptyMap())

            viewModel.sideEffect.test {
                viewModel.handleIntent(AiIntent.AskAi("Create meeting", "en"))
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is AiSideEffect.RequireUserConfirmation)
                assertEquals(createIntent, viewModel.uiState.value.pendingAIAction)
                cancelAndConsumeRemainingEvents()
            }
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
                mockProcessAiResponseUseCase(
                    "create_event",
                    any(),
                )
            } returns AIToolResult.Success(criticalTool, createIntent)

            coEvery { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                AIAgentResponse.FunctionCall("create_event", emptyMap())

            viewModel.handleIntent(AiIntent.AskAi("Create meeting", "en"))
            advanceUntilIdle()

            viewModel.handleIntent(AiIntent.ApprovePendingAction)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingAIAction)
            coVerify { mockCreateEventUseCase(1L, "Meeting", any(), any(), 1000L, 2000L, false, any()) }
        }

    @Test
    fun `ToggleFocusMode intent enables DND when permission granted`() =
        runTest {
            every { mockToggleFocusModeUseCase(true) } returns Result.success(Unit)

            viewModel.handleIntent(AiIntent.ToggleFocusMode(true))
            advanceUntilIdle()

            verify { mockToggleFocusModeUseCase(true) }
        }
}
