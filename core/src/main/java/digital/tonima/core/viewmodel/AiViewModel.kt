package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.collect.ImmutableMap
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.AIToolResult.InvalidArguments
import digital.tonima.core.ai.AIToolResult.Success
import digital.tonima.core.ai.AIToolResult.ToolNotFound
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.model.ChatMessage.FunctionResponse
import digital.tonima.core.ai.model.ChatMessage.Text
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
import digital.tonima.core.viewmodel.AiIntent.AnalyzeSchedule
import digital.tonima.core.viewmodel.AiIntent.ApprovePendingAction
import digital.tonima.core.viewmodel.AiIntent.AskAi
import digital.tonima.core.viewmodel.AiIntent.CategorizeEvent
import digital.tonima.core.viewmodel.AiIntent.ClearAiResponse
import digital.tonima.core.viewmodel.AiIntent.CloseChatDetail
import digital.tonima.core.viewmodel.AiIntent.CloseChatHistoryScreen
import digital.tonima.core.viewmodel.AiIntent.ConsumeEffect
import digital.tonima.core.viewmodel.AiIntent.CreateFocusBlock
import digital.tonima.core.viewmodel.AiIntent.CreateNewChat
import digital.tonima.core.viewmodel.AiIntent.DeleteChat
import digital.tonima.core.viewmodel.AiIntent.DismissAiSuggestionsDialog
import digital.tonima.core.viewmodel.AiIntent.GenerateDailyBriefing
import digital.tonima.core.viewmodel.AiIntent.NotifyRunningLate
import digital.tonima.core.viewmodel.AiIntent.OpenChatDetail
import digital.tonima.core.viewmodel.AiIntent.OpenChatHistoryScreen
import digital.tonima.core.viewmodel.AiIntent.RejectPendingAction
import digital.tonima.core.viewmodel.AiIntent.RescheduleEvent
import digital.tonima.core.viewmodel.AiIntent.ShowAiSuggestionsDialog
import digital.tonima.core.viewmodel.AiIntent.SpeakAiResponse
import digital.tonima.core.viewmodel.AiIntent.StopSpeaking
import digital.tonima.core.viewmodel.AiIntent.SummarizeMeetTranscript
import digital.tonima.core.viewmodel.AiIntent.ToggleFocusMode
import digital.tonima.core.viewmodel.AiSideEffect.AIToolError
import digital.tonima.core.viewmodel.AiSideEffect.RequireUserConfirmation
import digital.tonima.core.viewmodel.AiSideEffect.ShowSnackbar
import digital.tonima.core.viewmodel.SettingsIntent.ToggleGlobalAlarms
import digital.tonima.core.viewmodel.UiText.DynamicString
import digital.tonima.core.viewmodel.UiText.StringResource
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.R.string.ai_agent_create_event_confirmation
import digital.tonima.kairos.core.R.string.ai_agent_create_event_with_location_confirmation
import digital.tonima.kairos.core.R.string.ai_agent_invalid_args
import digital.tonima.kairos.core.R.string.ai_agent_snackbar_executed
import digital.tonima.kairos.core.R.string.ai_agent_tool_not_found
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth.now
import java.time.ZoneId
import java.util.Locale.getDefault
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class AiViewModel
    @Inject
    constructor(
        proUserProvider: ProUserProvider,
        private val askAiAgentUseCase: AskAiAgentUseCase,
        private val observeDailyBriefingUseCase: ObserveDailyBriefingUseCase,
        private val generateDailyBriefingUseCase: GenerateDailyBriefingUseCase,
        private val getRegisteredAiToolsUseCase: GetRegisteredAiToolsUseCase,
        private val processAiResponseUseCase: ProcessAiResponseUseCase,
        private val speakTextUseCase: SpeakTextUseCase,
        private val updateWidgetUseCase: UpdateWidgetUseCase,
        private val observeChatHistoryUseCase: ObserveChatHistoryUseCase,
        private val getChatHistoryUseCase: GetChatHistoryUseCase,
        private val insertChatMessageUseCase: InsertChatMessageUseCase,
        private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
        private val observeConversationsUseCase: ObserveConversationsUseCase,
        private val createConversationUseCase: CreateConversationUseCase,
        private val deleteConversationUseCase: DeleteConversationUseCase,
        private val fetchMeetingTranscriptUseCase: FetchMeetingTranscriptUseCase,
        private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
        private val toggleFocusModeUseCase: ToggleFocusModeUseCase,
        private val createEventUseCase: CreateEventUseCase,
        private val getAvailableCalendarsUseCase: GetAvailableCalendarsUseCase,
    ) : ViewModel(), ProUserProvider by proUserProvider {
        private val _uiState = MutableStateFlow(AiUiState())
        val uiState = _uiState.asStateFlow()

        val effect = uiState.map { it.effect }.distinctUntilChanged()

        private var chatHistoryJob: Job? = null

        init {
            observeDailyBriefing()
            observeConversations()
        }

        fun handleIntent(intent: AiIntent) {
            viewModelScope.launch {
                when (intent) {
                    is ConsumeEffect -> _uiState.update { it.copy(effect = null) }
                    is AskAi -> askAi(intent.question, intent.language)
                    is GenerateDailyBriefing -> generateDailyBriefing(intent.language)
                    SpeakAiResponse -> speakAiResponse()
                    StopSpeaking -> stopSpeaking()
                    ClearAiResponse -> clearAiResponse()
                    OpenChatHistoryScreen -> _uiState.update { it.copy(showChatHistoryScreen = true) }
                    CloseChatHistoryScreen -> {
                        _uiState.update {
                            it.copy(showChatHistoryScreen = false, selectedConversationId = null)
                        }
                        chatHistoryJob?.cancel()
                    }
                    is OpenChatDetail -> {
                        _uiState.update { it.copy(selectedConversationId = intent.conversationId) }
                        observeChatHistory(intent.conversationId)
                    }
                    CloseChatDetail -> {
                        _uiState.update { it.copy(selectedConversationId = null) }
                        chatHistoryJob?.cancel()
                    }
                    is CreateNewChat -> {
                        val id = createConversationUseCase(intent.title)
                        handleIntent(OpenChatDetail(id))
                    }
                    is DeleteChat -> {
                        deleteConversationUseCase(intent.conversationId)
                        if (_uiState.value.selectedConversationId == intent.conversationId) {
                            handleIntent(CloseChatDetail)
                        }
                    }
                    ApprovePendingAction -> executePendingAction()
                    RejectPendingAction -> rejectPendingAction()
                    is NotifyRunningLate -> handleNotifyRunningLate(intent)
                    is ToggleFocusMode -> handleToggleFocusMode(intent)
                    is CreateFocusBlock -> handleCreateFocusBlock(intent)
                    is SummarizeMeetTranscript -> handleSummarizeMeetTranscript(intent)
                    ShowAiSuggestionsDialog ->
                        _uiState.update {
                            it.copy(showAiSuggestionsDialog = true)
                        }
                    DismissAiSuggestionsDialog ->
                        _uiState.update {
                            it.copy(showAiSuggestionsDialog = false)
                        }
                    is AnalyzeSchedule -> handleMappedIntent(intent)
                    is CategorizeEvent -> handleMappedIntent(intent)
                    is RescheduleEvent -> handleMappedIntent(intent)
                }
            }
        }

        private fun observeDailyBriefing() {
            observeDailyBriefingUseCase().onEach { briefing ->
                _uiState.update { it.copy(dailyBriefing = briefing) }
            }.launchIn(viewModelScope)
        }

        private fun observeConversations() {
            observeConversationsUseCase().onEach { list ->
                _uiState.update { it.copy(conversations = list) }
            }.launchIn(viewModelScope)
        }

        private fun observeChatHistory(conversationId: Long) {
            chatHistoryJob?.cancel()
            chatHistoryJob =
                observeChatHistoryUseCase(conversationId).onEach { messages ->
                    _uiState.update { it.copy(chatHistory = messages) }
                }.launchIn(viewModelScope)
        }

        private fun askAi(
            question: String?,
            language: String = getDefault().language,
        ) {
            viewModelScope.launch {
                var convId = _uiState.value.selectedConversationId
                if (convId == null) {
                    convId = createConversationUseCase(question ?: "Nova Conversa")
                    _uiState.update { it.copy(selectedConversationId = convId) }
                    observeChatHistory(convId)
                }

                val currentHistory = getChatHistoryUseCase(convId)

                if (!question.isNullOrBlank()) {
                    val questionMsg = Text(ChatMessage.Role.USER, question)
                    insertChatMessageUseCase(convId, questionMsg)
                }

                _uiState.update {
                    it.copy(
                        isAskingAi = true,
                        aiResponse = null,
                        lastAiQuestion = question ?: it.lastAiQuestion,
                    )
                }
                // Note: This still needs a way to get events. For now, we'll fetch them here.
                // Ideally, the UseCase should handle its own data fetching or we pass it.
                // In the original, it was using _uiState.value.currentMonth.
                // Since AiViewModel doesn't track currentMonth, we'll use current date's month.
                val eventsRecent = getEventsForMonthUseCase(now().atDay(1).toEpochDay())

                val agentResponse =
                    askAiAgentUseCase(
                        eventsRecent,
                        question,
                        language,
                        getRegisteredAiToolsUseCase(),
                        currentHistory,
                    )

                when (agentResponse) {
                    is AIAgentResponse.Text -> {
                        val answerMsg = Text(ChatMessage.Role.ASSISTANT, agentResponse.content)
                        insertChatMessageUseCase(convId, answerMsg)
                        processAiResponse(agentResponse.content)
                    }
                    is AIAgentResponse.FunctionCall -> {
                        val callMsg =
                            ChatMessage.FunctionCall(
                                agentResponse.name,
                                ImmutableMap.copyOf(agentResponse.args),
                            )
                        insertChatMessageUseCase(convId, callMsg)
                        onAIFunctionCalled(agentResponse.name, agentResponse.args)
                    }
                    is AIAgentResponse.Empty -> Unit
                }
                _uiState.update { it.copy(isAskingAi = false) }
            }
        }

        private fun processAiResponse(response: String) {
            val trimmedResponse = response.trim()
            val hasJsonStart = trimmedResponse.contains("\"title\":") && trimmedResponse.contains("{")

            if (hasJsonStart) {
                parseVoiceEventData(trimmedResponse)?.let { voiceEventData ->
                    _uiState.update { it.copy(voiceEventData = voiceEventData) }
                    // In EventViewModel this triggered ShowCreateEventDialog.
                    // We'll let the UI observe voiceEventData.
                    return
                }
            }
            _uiState.update { it.copy(aiResponse = response) }
            speak(response)
        }

        private fun parseVoiceEventData(jsonStr: String): VoiceEventData? {
            return try {
                val title =
                    Regex(
                        "\"title\":\\s*\"([^\"]+)\"",
                    ).find(jsonStr)?.groupValues?.get(1) ?: return null
                val description =
                    Regex(
                        "\"description\":\\s*\"([^\"]+)\"",
                    ).find(jsonStr)?.groupValues?.get(1)
                val location =
                    Regex(
                        "\"location\":\\s*\"([^\"]+)\"",
                    ).find(jsonStr)?.groupValues?.get(1)
                val startTime =
                    Regex(
                        "\"startTime\":\\s*(\\d+)",
                    ).find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
                val endTime =
                    Regex(
                        "\"endTime\":\\s*(\\d+)",
                    ).find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
                val isAllDay =
                    Regex(
                        "\"isAllDay\":\\s*(true|false)",
                    ).find(jsonStr)?.groupValues?.get(1)?.toBoolean() ?: false
                VoiceEventData(title, description, location, startTime, endTime, isAllDay)
            } catch (_: Exception) {
                null
            }
        }

        private fun speak(text: String) {
            _uiState.update { it.copy(isSpeaking = true) }
            speakTextUseCase(text) { _uiState.update { it.copy(isSpeaking = false) } }
        }

        private fun speakAiResponse() {
            _uiState.value.aiResponse?.let { speak(it) }
        }

        private fun stopSpeaking() {
            speakTextUseCase.stop()
            _uiState.update { it.copy(isSpeaking = false) }
        }

        private fun clearAiResponse() {
            viewModelScope.launch {
                val convId = _uiState.value.selectedConversationId
                if (convId != null) {
                    clearChatHistoryUseCase(convId)
                }
                _uiState.update {
                    it.copy(
                        aiResponse = null,
                        chatHistory = emptyList(),
                        lastAiQuestion = null,
                    )
                }
                stopSpeaking()
            }
        }

        private fun generateDailyBriefing(language: String) {
            // This needs events. In original it used events from uiState.
            // We'll fetch them here.
            viewModelScope.launch {
                _uiState.update { it.copy(isGeneratingBriefing = true) }
                val eventsToday =
                    getEventsForMonthUseCase(
                        now().atDay(1).toEpochDay(),
                    ).filter {
                        val date = Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                        date == LocalDate.now()
                    }
                val briefing = generateDailyBriefingUseCase(eventsToday, language)
                if (briefing != null) {
                    updateWidgetUseCase.updateDailyBriefingWidget()
                }
                _uiState.update { it.copy(isGeneratingBriefing = false) }
            }
        }

        private fun onAIFunctionCalled(
            toolName: String,
            args: Map<String, Any?>,
        ) {
            viewModelScope.launch {
                var convId = _uiState.value.selectedConversationId
                if (convId == null) {
                    convId = createConversationUseCase("Nova Conversa")
                    _uiState.update { it.copy(selectedConversationId = convId) }
                    observeChatHistory(convId)
                }
                when (val result = processAiResponseUseCase(toolName, args)) {
                    is Success -> {
                        routeByRiskLevel(result)
                        val responseMsg =
                            FunctionResponse(
                                toolName,
                                ImmutableMap.copyOf(
                                    mapOf("status" to "success", "message" to "Intent gerado e processado"),
                                ),
                            )
                        insertChatMessageUseCase(convId, responseMsg)
                        askAi(null)
                    }
                    is ToolNotFound -> {
                        val responseMsg =
                            FunctionResponse(
                                toolName,
                                ImmutableMap.copyOf(
                                    mapOf("error" to "Tool not found"),
                                ),
                            )
                        insertChatMessageUseCase(convId, responseMsg)
                        askAi(null)
                        _uiState.update {
                            it.copy(
                                effect =
                                    AIToolError(
                                        StringResource(
                                            ai_agent_tool_not_found,
                                            listOf(result.toolName),
                                        ),
                                    ),
                            )
                        }
                    }
                    is InvalidArguments -> {
                        val responseMsg =
                            FunctionResponse(
                                toolName,
                                ImmutableMap.copyOf(
                                    mapOf("error" to "Invalid arguments"),
                                ),
                            )
                        insertChatMessageUseCase(convId, responseMsg)
                        askAi(null)
                        _uiState.update {
                            it.copy(
                                effect =
                                    AIToolError(
                                        StringResource(
                                            ai_agent_invalid_args,
                                            listOf(result.toolName),
                                        ),
                                    ),
                            )
                        }
                    }
                }
            }
        }

        private fun routeByRiskLevel(result: Success) {
            val tool = result.tool
            val intent = result.intent

            when (tool.riskLevel) {
                RiskLevel.SAFE -> handleMappedIntent(intent)
                RiskLevel.MODERATE -> {
                    handleMappedIntent(intent)
                    _uiState.update {
                        it.copy(
                            effect =
                                ShowSnackbar(
                                    StringResource(
                                        ai_agent_snackbar_executed,
                                        listOf(tool.name),
                                    ),
                                ),
                        )
                    }
                }
                RiskLevel.CRITICAL -> {
                    _uiState.update {
                        it.copy(
                            pendingAIAction = intent,
                            effect =
                                RequireUserConfirmation(
                                    title = StringResource(R.string.ai_agent_confirmation_title),
                                    message = formatConfirmationMessage(tool, intent),
                                ),
                        )
                    }
                }
            }
        }

        private fun handleMappedIntent(intent: BaseIntent) {
            // This handles any BaseIntent generated by the AI agent tools.
            when (intent) {
                is NotifyRunningLate -> handleNotifyRunningLate(intent)
                is ToggleFocusMode -> handleToggleFocusMode(intent)
                is CreateFocusBlock -> handleCreateFocusBlock(intent)
                is AnalyzeSchedule -> {
                    _uiState.update {
                        it.copy(
                            effect =
                                ShowSnackbar(
                                    DynamicString(
                                        "Analysing schedule for ${intent.timeframe}...",
                                    ),
                                ),
                        )
                    }
                }
                is CategorizeEvent -> {
                    _uiState.update {
                        it.copy(
                            effect =
                                ShowSnackbar(
                                    DynamicString(
                                        "Event categorized: ${intent.category}",
                                    ),
                                ),
                        )
                    }
                }
                is RescheduleEvent -> {
                    _uiState.update {
                        it.copy(
                            effect = ShowSnackbar(DynamicString("Event rescheduled.")),
                        )
                    }
                }
                is SummarizeMeetTranscript -> handleSummarizeMeetTranscript(intent)
                is EventIntent.CreateEvent -> {
                    viewModelScope.launch {
                        createEventUseCase(
                            calendarId = intent.calendarId,
                            title = intent.title,
                            description = intent.description,
                            location = intent.location,
                            startTime = intent.startTime,
                            endTime = intent.endTime,
                            isAllDay = intent.isAllDay,
                            requestMeetLink = intent.requestMeetLink,
                        )
                    }
                }
                is ToggleGlobalAlarms -> {
                    _uiState.update {
                        it.copy(
                            effect =
                                ShowSnackbar(
                                    DynamicString(
                                        "Global alarms ${if (intent.enabled) "enabled" else "disabled"} " +
                                            "by AI",
                                    ),
                                ),
                        )
                    }
                }
                else -> {
                    logcat { "AI Agent generated unhandled intent: ${intent::class.simpleName}" }
                }
            }
        }

        private fun executePendingAction() {
            val pending = _uiState.value.pendingAIAction ?: return
            _uiState.update { it.copy(pendingAIAction = null) }
            handleMappedIntent(pending)
        }

        private fun rejectPendingAction() {
            _uiState.update { it.copy(pendingAIAction = null) }
        }

        private fun handleNotifyRunningLate(intent: NotifyRunningLate) {
            _uiState.update {
                it.copy(
                    effect =
                        ShowSnackbar(
                            StringResource(
                                R.string.ai_suggested_late_notification,
                                listOf(intent.message),
                            ),
                        ),
                )
            }
        }

        private fun handleToggleFocusMode(intent: ToggleFocusMode) {
            toggleFocusModeUseCase(intent.enabled).onSuccess {
                val status = if (intent.enabled) "enabled" else "disabled"
                _uiState.update {
                    it.copy(
                        effect =
                            ShowSnackbar(
                                StringResource(
                                    ai_agent_snackbar_executed,
                                    listOf("DND $status"),
                                ),
                            ),
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        effect =
                            AIToolError(
                                DynamicString(
                                    "Permission for DND access required. " +
                                        "Please enable it in system settings.",
                                ),
                            ),
                    )
                }
            }
        }

        private fun handleCreateFocusBlock(intent: CreateFocusBlock) {
            viewModelScope.launch {
                val calendars = getAvailableCalendarsUseCase()
                val calendarId = calendars.firstOrNull()?.id ?: return@launch
                createEventUseCase(
                    calendarId = calendarId,
                    title = intent.title,
                    description = "Gerado por AI para Foco",
                    location = null,
                    startTime = intent.startTime,
                    endTime = intent.endTime,
                    isAllDay = false,
                    requestMeetLink = false,
                )
                _uiState.update {
                    it.copy(
                        effect = ShowSnackbar(StringResource(R.string.ai_agent_event_created)),
                    )
                }
            }
        }

        private fun handleSummarizeMeetTranscript(intent: SummarizeMeetTranscript) {
            viewModelScope.launch {
                val result = fetchMeetingTranscriptUseCase(intent.meetingUrl)
                result.onSuccess { transcript ->
                    var convId = _uiState.value.selectedConversationId
                    if (convId == null) {
                        convId = createConversationUseCase("Resumo Reunião")
                        _uiState.update { it.copy(selectedConversationId = convId) }
                        observeChatHistory(convId)
                    }
                    val questionMsg =
                        Text(
                            ChatMessage.Role.USER,
                            "Aqui está a transcrição da reunião: \n$transcript\n\n" +
                                "Por favor, resuma os principais pontos discutidos e extraia as ações (action items).",
                        )
                    insertChatMessageUseCase(convId, questionMsg)
                    askAi(null)
                }.onFailure { e ->
                    val errorMsg = "Falha ao baixar transcrição: ${e.message}"
                    _uiState.update {
                        it.copy(
                            effect = AIToolError(DynamicString(errorMsg)),
                        )
                    }
                }
            }
        }

        private fun formatConfirmationMessage(
            tool: AITool,
            intent: BaseIntent,
        ): UiText =
            when (intent) {
                is EventIntent.CreateEvent ->
                    if (intent.location != null) {
                        StringResource(
                            ai_agent_create_event_with_location_confirmation,
                            listOf(intent.title, intent.location),
                        )
                    } else {
                        StringResource(ai_agent_create_event_confirmation, listOf(intent.title))
                    }
                else -> StringResource(R.string.ai_agent_generic_confirmation, listOf(tool.name))
            }
    }
