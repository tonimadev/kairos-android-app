package digital.tonima.core.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.AIToolResult
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.usecases.AskAiAgentUseCase
import digital.tonima.core.ai.usecases.BriefingResult
import digital.tonima.core.ai.usecases.ClearChatHistoryUseCase
import digital.tonima.core.ai.usecases.CreateConversationUseCase
import digital.tonima.core.ai.usecases.DeleteConversationUseCase
import digital.tonima.core.ai.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.ai.usecases.GetChatHistoryUseCase
import digital.tonima.core.ai.usecases.GetRegisteredAiToolsUseCase
import digital.tonima.core.ai.usecases.InsertChatMessageUseCase
import digital.tonima.core.ai.usecases.ObserveChatHistoryUseCase
import digital.tonima.core.ai.usecases.ObserveConversationsUseCase
import digital.tonima.core.ai.usecases.ObserveDailyBriefingUseCase
import digital.tonima.core.ai.usecases.ProcessAiResponseUseCase
import digital.tonima.core.ai.usecases.SpeakTextUseCase
import digital.tonima.core.ai.usecases.UpdateWidgetUseCase
import digital.tonima.core.data.usecases.CreateEventUseCase
import digital.tonima.core.data.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.data.usecases.GetEventsForMonthUseCase
import digital.tonima.core.data.usecases.RescheduleEventUseCase
import digital.tonima.core.data.usecases.RescheduleResult
import digital.tonima.core.data.usecases.ToggleFocusModeUseCase
import digital.tonima.core.database.entity.ConversationEntity
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.feature.ai.bridge.AiNavKey
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.DeviceCalendar
import digital.tonima.kairos.core.model.Event
import digital.tonima.kairos.core.navigation.AppNavigator
import digital.tonima.kairos.core.navigation.BaseIntent
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.ZoneId
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
    private val mockGetEventsForMonthUseCase: GetEventsForMonthUseCase = mockk(relaxed = true)
    private val mockToggleFocusModeUseCase: ToggleFocusModeUseCase = mockk(relaxed = true)
    private val mockCreateEventUseCase: CreateEventUseCase = mockk(relaxed = true)
    private val mockGetAvailableCalendarsUseCase: GetAvailableCalendarsUseCase = mockk(relaxed = true)
    private val mockRescheduleEventUseCase: RescheduleEventUseCase = mockk()
    private val mockAppNavigator: AppNavigator = mockk(relaxed = true)

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

        every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns flowOf(AIAgentResponse.Text(""))

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
            getEventsForMonthUseCase = mockGetEventsForMonthUseCase,
            toggleFocusModeUseCase = mockToggleFocusModeUseCase,
            createEventUseCase = mockCreateEventUseCase,
            getAvailableCalendarsUseCase = mockGetAvailableCalendarsUseCase,
            rescheduleEventUseCase = mockRescheduleEventUseCase,
            appNavigator = mockAppNavigator,
        )

    @Test
    fun `generateDailyBriefing calls usecase and updates widget`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()

            coEvery { mockGenerateDailyBriefingUseCase(any(), any()) } returns
                BriefingResult.Success("Briefing content")
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

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(AIAgentResponse.Text("AI Response"))

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

            val response1 = flowOf(AIAgentResponse.Text("Response 1"))
            val response2 = flowOf(AIAgentResponse.Text("Response 2"))

            every {
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

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(AIAgentResponse.Text("Some response"))

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

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    flowOf(
                        AIAgentResponse.FunctionCall(
                            "notify_late",
                            mapOf("eventId" to "test_event", "message" to "Running late!"),
                        ),
                    ),
                    flowOf(AIAgentResponse.Text("Pronto, avisei que você vai se atrasar.")),
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

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    flowOf(AIAgentResponse.FunctionCall("create_event", emptyMap())),
                    flowOf(AIAgentResponse.Text("Aguardando sua confirmação para criar o evento.")),
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

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    flowOf(AIAgentResponse.FunctionCall("create_event", emptyMap())),
                    flowOf(AIAgentResponse.Text("Pronto.")),
                )

            viewModel.handleIntent(AiIntent.AskAi("Create meeting", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            viewModel.handleIntent(AiIntent.ApprovePendingAction)
            runCurrent()

            assertNull(viewModel.uiState.value.pendingAIAction)
            coVerify { mockCreateEventUseCase(1L, "Meeting", any(), any(), 1000L, 2000L, false) }
            assertTrue(viewModel.uiState.value.effect is AiSideEffect.CalendarEventCreated)
        }

    @Test
    fun `ApprovePendingAction emits AIToolError when the event cannot be created`() =
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
            coEvery { mockCreateEventUseCase(any(), any(), any(), any(), any(), any(), any()) } returns null

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    flowOf(AIAgentResponse.FunctionCall("create_event", emptyMap())),
                    flowOf(AIAgentResponse.Text("Pronto.")),
                )

            viewModel.handleIntent(AiIntent.AskAi("Create meeting", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            viewModel.handleIntent(AiIntent.ApprovePendingAction)
            runCurrent()

            assertTrue(viewModel.uiState.value.effect is AiSideEffect.AIToolError)
        }

    @Test
    fun `ToggleFocusMode intent enables DND when permission granted`() =
        runTest {
            every { mockToggleFocusModeUseCase(true) } returns Result.success(Unit)

            viewModel.handleIntent(AiIntent.ToggleFocusMode(true))
            runCurrent()

            verify { mockToggleFocusModeUseCase(true) }
        }

    @Test
    fun `askAi surfaces a snackbar and clears loading state on Error`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(AIAgentResponse.Error(UiText.DynamicString("network down")))

            viewModel.handleIntent(AiIntent.AskAi("What's next?", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state.effect is AiSideEffect.AIToolError)
            assertFalse(state.isAskingAi)
            assertNull(state.streamingText)
        }

    @Test
    fun `askAi updates streamingText as chunks arrive and clears it when done`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(
                    AIAgentResponse.Text("Hello"),
                    AIAgentResponse.Text("Hello world"),
                )

            viewModel.handleIntent(AiIntent.AskAi("Hi", "en"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals("Hello world", state.aiResponse)
            assertNull(state.streamingText)
        }

    @Test
    fun `an event dictated by voice opens the create dialog instead of being read aloud`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()
            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(
                    AIAgentResponse.Text(
                        """{"title": "Dentista", "description": "Levar exames", "location": "Rua Augusta", """ +
                            """"startTime": 1800000000000, "endTime": 1800003600000, "isAllDay": false}""",
                    ),
                )

            viewModel.handleIntent(AiIntent.AskAi("Marque dentista amanhã às 10h", "pt"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(
                VoiceEventData(
                    title = "Dentista",
                    description = "Levar exames",
                    location = "Rua Augusta",
                    startTime = 1_800_000_000_000L,
                    endTime = 1_800_003_600_000L,
                    isAllDay = false,
                ),
                state.voiceEventData,
            )
            assertNull("The raw JSON must not be shown or spoken", state.aiResponse)
        }

    @Test
    fun `a dictated all-day event with only a title keeps the optional fields empty`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()
            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(AIAgentResponse.Text("""{"title": "Feriado", "isAllDay": true}"""))

            viewModel.handleIntent(AiIntent.AskAi("Feriado amanhã", "pt"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            assertEquals(VoiceEventData(title = "Feriado", isAllDay = true), viewModel.uiState.value.voiceEventData)
        }

    @Test
    fun `json-looking text without a title is shown as a normal answer`() =
        runTest {
            isAiUserFlow.value = true
            runCurrent()
            val answer = """Use {"title": } to name it"""
            every {
                mockAskAiAgentUseCase(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns flowOf(AIAgentResponse.Text(answer))

            viewModel.handleIntent(AiIntent.AskAi("?", "pt"))
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            assertEquals(answer, viewModel.uiState.value.aiResponse)
            assertNull(viewModel.uiState.value.voiceEventData)
        }

    @Test
    fun `a focus block is created in the first calendar`() =
        runTest {
            coEvery { mockGetAvailableCalendarsUseCase() } returns
                listOf(DeviceCalendar(id = 5L, displayName = "Work", accountName = "me@company.com"))
            coEvery { mockCreateEventUseCase(any(), any(), any(), any(), any(), any(), any()) } returns 99L

            viewModel.handleIntent(AiIntent.CreateFocusBlock(startTime = 1_000L, endTime = 2_000L, title = "Foco"))
            runCurrent()

            coVerify {
                mockCreateEventUseCase(
                    calendarId = 5L,
                    title = "Foco",
                    description = any(),
                    location = null,
                    startTime = 1_000L,
                    endTime = 2_000L,
                    isAllDay = false,
                )
            }
            assertTrue(viewModel.uiState.value.effect is AiSideEffect.CalendarEventCreated)
        }

    @Test
    fun `a focus block is not created without any calendar`() =
        runTest {
            coEvery { mockGetAvailableCalendarsUseCase() } returns emptyList()

            viewModel.handleIntent(AiIntent.CreateFocusBlock(startTime = 1_000L, endTime = 2_000L))
            runCurrent()

            coVerify(exactly = 0) { mockCreateEventUseCase(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `a failed focus block creation is reported`() =
        runTest {
            coEvery { mockGetAvailableCalendarsUseCase() } returns
                listOf(DeviceCalendar(id = 5L, displayName = "Work", accountName = "a"))
            coEvery { mockCreateEventUseCase(any(), any(), any(), any(), any(), any(), any()) } returns null

            viewModel.handleIntent(AiIntent.CreateFocusBlock(startTime = 1_000L, endTime = 2_000L))
            runCurrent()

            assertTrue(viewModel.uiState.value.effect is AiSideEffect.AIToolError)
        }

    @Test
    fun `focus mode without do-not-disturb access explains what is missing`() =
        runTest {
            every { mockToggleFocusModeUseCase(true) } returns Result.failure(SecurityException("no DND access"))

            viewModel.handleIntent(AiIntent.ToggleFocusMode(true))
            runCurrent()

            assertTrue(viewModel.uiState.value.effect is AiSideEffect.AIToolError)
        }

    @Test
    fun `suggestions dialog can be shown and dismissed`() =
        runTest {
            viewModel.handleIntent(AiIntent.ShowAiSuggestionsDialog)
            runCurrent()
            assertTrue(viewModel.uiState.value.showAiSuggestionsDialog)

            viewModel.handleIntent(AiIntent.DismissAiSuggestionsDialog)
            runCurrent()
            assertFalse(viewModel.uiState.value.showAiSuggestionsDialog)
        }

    @Test
    fun `deleting the open conversation also closes it`() =
        runTest {
            viewModel.handleIntent(AiIntent.OpenChatDetail(7L))
            runCurrent()

            viewModel.handleIntent(AiIntent.DeleteChat(7L))
            runCurrent()

            coVerify { mockDeleteConversationUseCase(7L) }
            assertNull(viewModel.uiState.value.selectedConversationId)
            verify { mockAppNavigator.popBackStack() }
        }

    @Test
    fun `deleting another conversation keeps the open one`() =
        runTest {
            viewModel.handleIntent(AiIntent.OpenChatDetail(7L))
            runCurrent()

            viewModel.handleIntent(AiIntent.DeleteChat(8L))
            runCurrent()

            assertEquals(7L, viewModel.uiState.value.selectedConversationId)
            verify(exactly = 0) { mockAppNavigator.popBackStack() }
        }

    @Test
    fun `a new chat with a first question asks it right away`() =
        runTest {
            isAiUserFlow.value = true
            coEvery { mockCreateConversationUseCase("Agenda") } returns 12L
            every {
                mockAskAiAgentUseCase(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns flowOf(AIAgentResponse.Text("ok"))

            viewModel.handleIntent(
                AiIntent.CreateNewChat("Agenda", initialQuestion = "O que tenho hoje?", language = "pt"),
            )
            runCurrent()
            advanceTimeBy(1000.milliseconds)
            runCurrent()

            assertEquals(12L, viewModel.uiState.value.selectedConversationId)
            verify { mockAppNavigator.navigateTo(AiNavKey.ChatDetail(12L)) }
            verify { mockAskAiAgentUseCase(any(), "O que tenho hoje?", "pt", any(), any()) }
        }

    @Test
    fun `rescheduling moves the event and refreshes the calendar`() =
        runTest {
            coEvery { mockRescheduleEventUseCase(42L, 5_000L, 6_000L) } returns RescheduleResult.Success

            viewModel.handleIntent(AiIntent.RescheduleEvent("42", 5_000L, 6_000L))
            runCurrent()

            coVerify { mockRescheduleEventUseCase(42L, 5_000L, 6_000L) }
            assertTrue(viewModel.uiState.value.effect is AiSideEffect.CalendarEventUpdated)
        }

    @Test
    fun `rescheduling failures are reported instead of claiming success`() =
        runTest {
            listOf(
                RescheduleResult.RecurringNotSupported,
                RescheduleResult.InvalidTime,
                RescheduleResult.Failed,
            ).forEach { result ->
                coEvery { mockRescheduleEventUseCase(any(), any(), any()) } returns result

                viewModel.handleIntent(AiIntent.RescheduleEvent("42", 5_000L, 6_000L))
                runCurrent()

                assertTrue(result.toString(), viewModel.uiState.value.effect is AiSideEffect.AIToolError)
            }
        }

    @Test
    fun `an unknown event id is rejected without touching the calendar`() =
        runTest {
            viewModel.handleIntent(AiIntent.RescheduleEvent("not-an-id", 5_000L, 6_000L))
            runCurrent()

            coVerify(exactly = 0) { mockRescheduleEventUseCase(any(), any(), any()) }
            assertTrue(viewModel.uiState.value.effect is AiSideEffect.AIToolError)
        }

    // region Navigation and simple intents

    @Test
    fun `ConsumeEffect clears the pending effect`() =
        runTest {
            viewModel.handleIntent(AiIntent.NotifyRunningLate("1", "late"))
            runCurrent()
            assertNotNull(viewModel.uiState.value.effect)

            viewModel.handleIntent(AiIntent.ConsumeEffect)
            runCurrent()

            assertNull(viewModel.uiState.value.effect)
        }

    @Test
    fun `chat screens open and close through the navigator`() =
        runTest {
            viewModel.handleIntent(AiIntent.OpenChatDetail(3L))
            runCurrent()
            viewModel.handleIntent(AiIntent.OpenChatHistoryScreen)
            runCurrent()
            verify { mockAppNavigator.navigateTo(AiNavKey.ChatHistory) }

            viewModel.handleIntent(AiIntent.CloseChatHistoryScreen)
            runCurrent()

            assertNull(viewModel.uiState.value.selectedConversationId)
            verify(exactly = 1) { mockAppNavigator.popBackStack() }

            viewModel.handleIntent(AiIntent.OpenChatDetail(4L))
            runCurrent()
            viewModel.handleIntent(AiIntent.CloseChatDetail)
            runCurrent()

            assertNull(viewModel.uiState.value.selectedConversationId)
            verify(exactly = 2) { mockAppNavigator.popBackStack() }
        }

    @Test
    fun `a new chat without a first question does not ask the AI`() =
        runTest {
            coEvery { mockCreateConversationUseCase("Vazio") } returns 5L

            viewModel.handleIntent(AiIntent.CreateNewChat("Vazio", initialQuestion = " ", language = "pt"))
            runCurrent()

            assertEquals(5L, viewModel.uiState.value.selectedConversationId)
            verify(exactly = 0) { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `NotifyRunningLate shows the suggested message`() =
        runTest {
            viewModel.handleIntent(AiIntent.NotifyRunningLate("1", "Chego em 10 min"))
            runCurrent()

            val effect = viewModel.uiState.value.effect as AiSideEffect.ShowSnackbar
            assertEquals(
                UiText.StringResource(R.string.ai_suggested_late_notification, listOf("Chego em 10 min")),
                effect.message,
            )
        }

    @Test
    fun `disabling focus mode reports it`() =
        runTest {
            every { mockToggleFocusModeUseCase(false) } returns Result.success(Unit)

            viewModel.handleIntent(AiIntent.ToggleFocusMode(false))
            runCurrent()

            val effect = viewModel.uiState.value.effect as AiSideEffect.ShowSnackbar
            assertEquals(
                UiText.StringResource(R.string.ai_agent_snackbar_executed, listOf("DND disabled")),
                effect.message,
            )
        }

    @Test
    fun `schedule analysis and categorization give feedback`() =
        runTest {
            viewModel.handleIntent(AiIntent.AnalyzeSchedule("week"))
            runCurrent()
            assertEquals(
                AiSideEffect.ShowSnackbar(UiText.DynamicString("Analysing schedule for week...")),
                viewModel.uiState.value.effect,
            )

            viewModel.handleIntent(AiIntent.CategorizeEvent("1", "Work"))
            runCurrent()
            assertEquals(
                AiSideEffect.ShowSnackbar(UiText.DynamicString("Event categorized: Work")),
                viewModel.uiState.value.effect,
            )
        }

    // endregion

    // region Speech

    @Test
    fun `SpeakAiResponse reads the last answer, if any, and StopSpeaking stops it`() =
        runTest {
            viewModel.handleIntent(AiIntent.SpeakAiResponse)
            runCurrent()
            verify(exactly = 0) { mockSpeakTextUseCase(any(), any()) }

            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(AIAgentResponse.Text("Resposta"))
            viewModel.handleIntent(AiIntent.AskAi("Oi", "pt"))
            runCurrent()

            viewModel.handleIntent(AiIntent.SpeakAiResponse)
            runCurrent()
            verify(exactly = 2) { mockSpeakTextUseCase("Resposta", any()) }
            assertTrue(viewModel.uiState.value.isSpeaking)

            viewModel.handleIntent(AiIntent.StopSpeaking)
            runCurrent()
            verify { mockSpeakTextUseCase.stop() }
            assertFalse(viewModel.uiState.value.isSpeaking)
        }

    @Test
    fun `speaking finishes when the speech engine calls back`() =
        runTest {
            every { mockSpeakTextUseCase(any(), any()) } answers { secondArg<(() -> Unit)?>()?.invoke() }
            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(AIAgentResponse.Text("Resposta"))

            viewModel.handleIntent(AiIntent.AskAi("Oi", "pt"))
            runCurrent()

            assertFalse(viewModel.uiState.value.isSpeaking)
        }

    @Test
    fun `clearing without an open conversation does not touch the history`() =
        runTest {
            viewModel.handleIntent(AiIntent.ClearAiResponse)
            runCurrent()

            coVerify(exactly = 0) { mockClearChatHistoryUseCase(any()) }
            assertNull(viewModel.uiState.value.aiResponse)
        }

    // endregion

    // region Agent responses

    @Test
    fun `an empty agent response stores nothing and stops loading`() =
        runTest {
            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(AIAgentResponse.Empty)

            viewModel.handleIntent(AiIntent.AskAi("Oi", "pt"))
            runCurrent()

            assertEquals(1, fakeChatHistory.size)
            assertFalse(viewModel.uiState.value.isAskingAi)
            assertNull(viewModel.uiState.value.aiResponse)
        }

    @Test
    fun `a dictated event keeps every field it was given`() =
        runTest {
            val json =
                "{\"title\": \"Dentista\", \"description\": \"Limpeza\", \"location\": \"Centro\", " +
                    "\"startTime\": 1000, \"endTime\": 2000, \"isAllDay\": true}"
            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returns
                flowOf(AIAgentResponse.Text(json))

            viewModel.handleIntent(AiIntent.AskAi("Marque dentista", "pt"))
            runCurrent()

            assertEquals(
                VoiceEventData("Dentista", "Limpeza", "Centro", 1000L, 2000L, true),
                viewModel.uiState.value.voiceEventData,
            )
        }

    @Test
    fun `a tool that does not exist is reported and answered back to the agent`() =
        runTest {
            coEvery { mockProcessAiResponseUseCase("ghost", any()) } returns AIToolResult.ToolNotFound("ghost")
            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    flowOf(AIAgentResponse.FunctionCall("ghost", emptyMap())),
                    flowOf(AIAgentResponse.Text("Desculpe.")),
                )

            viewModel.handleIntent(AiIntent.AskAi("Faça algo", "pt"))
            runCurrent()

            assertTrue(
                fakeChatHistory.any {
                    it is ChatMessage.FunctionResponse && it.response == mapOf("error" to "Tool not found")
                },
            )
            assertEquals(
                AiSideEffect.AIToolError(UiText.StringResource(R.string.ai_agent_tool_not_found, listOf("ghost"))),
                viewModel.uiState.value.effect,
            )
        }

    @Test
    fun `invalid tool arguments are reported and answered back to the agent`() =
        runTest {
            coEvery { mockProcessAiResponseUseCase("create_event", any()) } returns
                AIToolResult.InvalidArguments("create_event", emptyMap())
            every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
                listOf(
                    flowOf(AIAgentResponse.FunctionCall("create_event", emptyMap())),
                    flowOf(AIAgentResponse.Text("Faltou informação.")),
                )

            viewModel.handleIntent(AiIntent.AskAi("Crie", "pt"))
            runCurrent()

            assertTrue(
                fakeChatHistory.any {
                    it is ChatMessage.FunctionResponse && it.response == mapOf("error" to "Invalid arguments")
                },
            )
            assertEquals(
                AiSideEffect.AIToolError(UiText.StringResource(R.string.ai_agent_invalid_args, listOf("create_event"))),
                viewModel.uiState.value.effect,
            )
        }

    @Test
    fun `a MODERATE tool runs and tells the user what it did`() =
        runTest {
            every { mockToggleFocusModeUseCase(true) } returns Result.success(Unit)
            callTool(RiskLevel.MODERATE, "focus", AiIntent.ToggleFocusMode(true))

            verify { mockToggleFocusModeUseCase(true) }
            assertEquals(
                AiSideEffect.ShowSnackbar(UiText.StringResource(R.string.ai_agent_snackbar_executed, listOf("focus"))),
                viewModel.uiState.value.effect,
            )
        }

    @Test
    fun `SAFE tools dispatch every kind of mapped intent`() =
        runTest {
            coEvery { mockGetAvailableCalendarsUseCase() } returns
                listOf(DeviceCalendar(id = 9L, displayName = "Pessoal", accountName = "a"))
            coEvery { mockCreateEventUseCase(any(), any(), any(), any(), any(), any(), any()) } returns 1L

            callTool(RiskLevel.SAFE, "focus_block", AiIntent.CreateFocusBlock(1_000L, 2_000L, "Foco"))
            coVerify { mockCreateEventUseCase(9L, "Foco", any(), null, 1_000L, 2_000L, false) }

            callTool(RiskLevel.SAFE, "analyze", AiIntent.AnalyzeSchedule("hoje"))
            assertEquals(
                AiSideEffect.ShowSnackbar(UiText.DynamicString("Analysing schedule for hoje...")),
                viewModel.uiState.value.effect,
            )

            callTool(RiskLevel.SAFE, "categorize", AiIntent.CategorizeEvent("1", "Saúde"))
            assertEquals(
                AiSideEffect.ShowSnackbar(UiText.DynamicString("Event categorized: Saúde")),
                viewModel.uiState.value.effect,
            )

            callTool(RiskLevel.SAFE, "alarms", SettingsIntent.ToggleGlobalAlarms(true))
            assertEquals(
                AiSideEffect.ShowSnackbar(UiText.DynamicString("Global alarms enabled by AI")),
                viewModel.uiState.value.effect,
            )

            callTool(RiskLevel.SAFE, "alarms", SettingsIntent.ToggleGlobalAlarms(false))
            assertEquals(
                AiSideEffect.ShowSnackbar(UiText.DynamicString("Global alarms disabled by AI")),
                viewModel.uiState.value.effect,
            )
        }

    @Test
    fun `an intent the agent cannot handle is ignored`() =
        runTest {
            callTool(RiskLevel.SAFE, "noop", AiIntent.ConsumeEffect)

            assertNull(viewModel.uiState.value.effect)
            assertNull(viewModel.uiState.value.pendingAIAction)
        }

    @Test
    fun `confirmation names the event and location, or else the tool`() =
        runTest {
            val intent = EventIntent.CreateEvent(1L, "Reunião", null, "Sala 2", 1000L, 2000L, false)
            callTool(RiskLevel.CRITICAL, "create_event", intent)

            val effect = viewModel.uiState.value.effect as AiSideEffect.RequireUserConfirmation
            assertEquals(
                UiText.StringResource(
                    R.string.ai_agent_create_event_with_location_confirmation,
                    listOf("Reunião", "Sala 2"),
                ),
                effect.message,
            )

            callTool(RiskLevel.CRITICAL, "toggle_alarms", SettingsIntent.ToggleGlobalAlarms(false))
            val generic = viewModel.uiState.value.effect as AiSideEffect.RequireUserConfirmation
            assertEquals(
                UiText.StringResource(R.string.ai_agent_generic_confirmation, listOf("toggle_alarms")),
                generic.message,
            )
        }

    @Test
    fun `rejecting a pending action discards it and approving nothing does nothing`() =
        runTest {
            viewModel.handleIntent(AiIntent.ApprovePendingAction)
            runCurrent()
            assertNull(viewModel.uiState.value.effect)

            callTool(RiskLevel.CRITICAL, "toggle_alarms", SettingsIntent.ToggleGlobalAlarms(false))
            assertNotNull(viewModel.uiState.value.pendingAIAction)

            viewModel.handleIntent(AiIntent.RejectPendingAction)
            runCurrent()

            assertNull(viewModel.uiState.value.pendingAIAction)
        }

    // endregion

    // region Daily briefing

    @Test
    fun `the daily briefing only uses today's events`() =
        runTest {
            val todayNoon =
                LocalDate.now().atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val today = Event(id = 1L, title = "Hoje", startTime = todayNoon)
            val other = Event(id = 2L, title = "Outro dia", startTime = todayNoon - 3 * 86_400_000L)
            coEvery { mockGetEventsForMonthUseCase(any()) } returns listOf(today, other)
            coEvery { mockGenerateDailyBriefingUseCase(any(), any()) } returns BriefingResult.Cached("ok")

            viewModel.handleIntent(AiIntent.GenerateDailyBriefing("pt"))
            runCurrent()

            coVerify { mockGenerateDailyBriefingUseCase(listOf(today), "pt") }
            coVerify(exactly = 0) { mockUpdateWidgetUseCase.updateDailyBriefingWidget() }
            assertFalse(viewModel.uiState.value.isGeneratingBriefing)
        }

    @Test
    fun `a failed daily briefing is reported`() =
        runTest {
            val message = UiText.DynamicString("sem rede")
            coEvery { mockGenerateDailyBriefingUseCase(any(), any()) } returns
                BriefingResult.Error(message, AiErrorType.NETWORK)

            viewModel.handleIntent(AiIntent.GenerateDailyBriefing("pt"))
            runCurrent()

            assertEquals(AiSideEffect.AIToolError(message), viewModel.uiState.value.effect)
            assertFalse(viewModel.uiState.value.isGeneratingBriefing)
        }

    // endregion

    private fun TestScope.callTool(
        riskLevel: RiskLevel,
        toolName: String,
        intent: BaseIntent,
    ) {
        val tool =
            mockk<AITool>(relaxed = true) {
                every { this@mockk.riskLevel } returns riskLevel
                every { name } returns toolName
            }
        coEvery { mockProcessAiResponseUseCase(toolName, any()) } returns AIToolResult.Success(tool, intent)
        every { mockAskAiAgentUseCase(any(), any(), any(), any(), any()) } returnsMany
            listOf(
                flowOf(AIAgentResponse.FunctionCall(toolName, emptyMap())),
                flowOf(AIAgentResponse.Text("Pronto.")),
            )
        viewModel.handleIntent(AiIntent.AskAi("Use $toolName", "pt"))
        runCurrent()
    }
}
