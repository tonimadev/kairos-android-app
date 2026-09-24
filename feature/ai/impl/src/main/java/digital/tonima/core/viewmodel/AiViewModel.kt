package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import digital.tonima.core.delegates.ProUserProvider
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
import digital.tonima.core.viewmodel.AiIntent.ToggleFocusMode
import digital.tonima.core.viewmodel.AiSideEffect.AIToolError
import digital.tonima.core.viewmodel.AiSideEffect.CalendarEventCreated
import digital.tonima.core.viewmodel.AiSideEffect.CalendarEventUpdated
import digital.tonima.core.viewmodel.AiSideEffect.RequireUserConfirmation
import digital.tonima.core.viewmodel.AiSideEffect.ShowSnackbar
import digital.tonima.core.viewmodel.SettingsIntent.ToggleGlobalAlarms
import digital.tonima.core.viewmodel.UiText.DynamicString
import digital.tonima.core.viewmodel.UiText.StringResource
import digital.tonima.feature.ai.bridge.AiNavKey
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.R.string.ai_agent_create_event_confirmation
import digital.tonima.kairos.core.R.string.ai_agent_create_event_with_location_confirmation
import digital.tonima.kairos.core.R.string.ai_agent_invalid_args
import digital.tonima.kairos.core.R.string.ai_agent_snackbar_executed
import digital.tonima.kairos.core.R.string.ai_agent_tool_not_found
import digital.tonima.kairos.core.navigation.AppNavigator
import digital.tonima.kairos.core.navigation.BaseIntent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
        private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
        private val toggleFocusModeUseCase: ToggleFocusModeUseCase,
        private val createEventUseCase: CreateEventUseCase,
        private val getAvailableCalendarsUseCase: GetAvailableCalendarsUseCase,
        private val rescheduleEventUseCase: RescheduleEventUseCase,
        private val appNavigator: AppNavigator,
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
                    OpenChatHistoryScreen -> appNavigator.navigateTo(AiNavKey.ChatHistory)
                    CloseChatHistoryScreen -> {
                        _uiState.update {
                            it.copy(selectedConversationId = null)
                        }
                        chatHistoryJob?.cancel()
                        appNavigator.popBackStack()
                    }
                    is OpenChatDetail -> {
                        _uiState.update { it.copy(selectedConversationId = intent.conversationId) }
                        observeChatHistory(intent.conversationId)
                        appNavigator.navigateTo(AiNavKey.ChatDetail(intent.conversationId))
                    }
                    CloseChatDetail -> {
                        _uiState.update { it.copy(selectedConversationId = null) }
                        chatHistoryJob?.cancel()
                        appNavigator.popBackStack()
                    }
                    is CreateNewChat -> {
                        val id = createConversationUseCase(intent.title)
                        _uiState.update { it.copy(selectedConversationId = id) }
                        observeChatHistory(id)
                        appNavigator.navigateTo(AiNavKey.ChatDetail(id))
                        if (!intent.initialQuestion.isNullOrBlank()) {
                            askAi(intent.initialQuestion, intent.language)
                        }
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

                var finalResponse: AIAgentResponse = AIAgentResponse.Empty
                askAiAgentUseCase(
                    eventsRecent,
                    question,
                    language,
                    getRegisteredAiToolsUseCase(),
                    currentHistory,
                ).collect { response ->
                    finalResponse = response
                    if (response is AIAgentResponse.Text) {
                        _uiState.update { it.copy(streamingText = response.content) }
                    }
                }

                when (val agentResponse = finalResponse) {
                    is AIAgentResponse.Text -> {
                        val answerMsg = Text(ChatMessage.Role.ASSISTANT, agentResponse.content)
                        insertChatMessageUseCase(convId, answerMsg)
                        processAiResponse(agentResponse.content)
                    }
                    is AIAgentResponse.FunctionCall -> {
                        val callMsg =
                            ChatMessage.FunctionCall(
                                agentResponse.name,
                                agentResponse.args.toMap(),
                            )
                        insertChatMessageUseCase(convId, callMsg)
                        onAIFunctionCalled(agentResponse.name, agentResponse.args)
                    }
                    is AIAgentResponse.Error -> {
                        _uiState.update { it.copy(effect = AIToolError(agentResponse.message)) }
                    }
                    is AIAgentResponse.Empty -> Unit
                }
                _uiState.update { it.copy(isAskingAi = false, streamingText = null) }
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
            return VoiceEventData(title, description, location, startTime, endTime, isAllDay)
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
                when (val result = generateDailyBriefingUseCase(eventsToday, language)) {
                    is BriefingResult.Success -> updateWidgetUseCase.updateDailyBriefingWidget()
                    is BriefingResult.Cached -> Unit
                    is BriefingResult.Error -> {
                        _uiState.update { it.copy(effect = AIToolError(result.message)) }
                    }
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
                                mapOf("status" to "success", "message" to "Intent gerado e processado"),
                            )
                        insertChatMessageUseCase(convId, responseMsg)
                        askAi(null)
                    }
                    is ToolNotFound -> {
                        val responseMsg =
                            FunctionResponse(
                                toolName,
                                mapOf("error" to "Tool not found"),
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
                                mapOf("error" to "Invalid arguments"),
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
                is RescheduleEvent -> handleRescheduleEvent(intent)
                is EventIntent.CreateEvent -> {
                    viewModelScope.launch {
                        val eventId =
                            createEventUseCase(
                                calendarId = intent.calendarId,
                                title = intent.title,
                                description = intent.description,
                                location = intent.location,
                                startTime = intent.startTime,
                                endTime = intent.endTime,
                                isAllDay = intent.isAllDay,
                            )
                        onCalendarEventCreated(eventId)
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

        private fun handleRescheduleEvent(intent: RescheduleEvent) {
            val eventId = intent.eventId.toLongOrNull()
            if (eventId == null) {
                _uiState.update {
                    it.copy(
                        effect =
                            AIToolError(
                                StringResource(R.string.ai_agent_invalid_args, listOf(RESCHEDULE_TOOL_NAME)),
                            ),
                    )
                }
                return
            }
            viewModelScope.launch {
                val effect =
                    when (rescheduleEventUseCase(eventId, intent.newStartTime, intent.newEndTime)) {
                        RescheduleResult.Success ->
                            CalendarEventUpdated(StringResource(R.string.ai_agent_event_rescheduled))
                        RescheduleResult.RecurringNotSupported ->
                            AIToolError(StringResource(R.string.ai_agent_reschedule_recurring_unsupported))
                        RescheduleResult.InvalidTime ->
                            AIToolError(StringResource(R.string.ai_agent_invalid_args, listOf(RESCHEDULE_TOOL_NAME)))
                        RescheduleResult.Failed ->
                            AIToolError(StringResource(R.string.ai_agent_reschedule_error))
                    }
                _uiState.update { it.copy(effect = effect) }
            }
        }

        private fun handleCreateFocusBlock(intent: CreateFocusBlock) {
            viewModelScope.launch {
                val calendars = getAvailableCalendarsUseCase()
                val calendarId = calendars.firstOrNull()?.id ?: return@launch
                val eventId =
                    createEventUseCase(
                        calendarId = calendarId,
                        title = intent.title,
                        description = "Gerado por AI para Foco",
                        location = null,
                        startTime = intent.startTime,
                        endTime = intent.endTime,
                        isAllDay = false,
                    )
                onCalendarEventCreated(eventId)
            }
        }

        private fun onCalendarEventCreated(eventId: Long?) {
            val effect =
                if (eventId != null) {
                    CalendarEventCreated(StringResource(R.string.ai_agent_event_created))
                } else {
                    AIToolError(StringResource(R.string.ai_agent_event_creation_error))
                }
            _uiState.update { it.copy(effect = effect) }
        }

        private fun formatConfirmationMessage(
            tool: AITool,
            intent: BaseIntent,
        ): UiText =
            when (intent) {
                is EventIntent.CreateEvent -> {
                    val location = intent.location
                    if (location != null) {
                        StringResource(
                            ai_agent_create_event_with_location_confirmation,
                            listOf(intent.title, location),
                        )
                    } else {
                        StringResource(ai_agent_create_event_confirmation, listOf(intent.title))
                    }
                }
                else -> StringResource(R.string.ai_agent_generic_confirmation, listOf(tool.name))
            }

        private companion object {
            const val RESCHEDULE_TOOL_NAME = "reschedule_event"
        }
    }
