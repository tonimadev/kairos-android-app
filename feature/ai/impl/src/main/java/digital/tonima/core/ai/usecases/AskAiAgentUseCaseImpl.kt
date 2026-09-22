package digital.tonima.core.ai.usecases

import android.content.Context
import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.SINGLETON
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.repository.AiModelRepository
import digital.tonima.core.ai.repository.AiModelResult
import digital.tonima.core.viewmodel.UiText
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@BindType(installIn = SINGLETON, to = AskAiAgentUseCase::class)
class AskAiAgentUseCaseImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val aiModelRepository: AiModelRepository,
    ) : AskAiAgentUseCase {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val eventTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        override fun invoke(
            events: List<Event>,
            question: String?,
            languageInstruction: String,
            availableTools: Set<AITool>,
            history: List<ChatMessage>,
        ): Flow<AIAgentResponse> {
            val systemInstructionText = buildSystemInstruction(events, languageInstruction)

            return aiModelRepository.streamAgentResponse(
                systemInstruction = systemInstructionText,
                availableTools = availableTools,
                history = history,
                question = question,
            ).map { result ->
                when (result) {
                    is AiModelResult.Text -> AIAgentResponse.Text(result.text)
                    is AiModelResult.FunctionCall -> AIAgentResponse.FunctionCall(result.name, result.args)
                    is AiModelResult.Error -> AIAgentResponse.Error(result.type.toUiText())
                }
            }
        }

        private fun AiErrorType.toUiText(): UiText =
            UiText.StringResource(
                when (this) {
                    AiErrorType.NETWORK -> R.string.ai_error_network
                    AiErrorType.RATE_LIMITED -> R.string.ai_error_rate_limited
                    AiErrorType.SAFETY_BLOCKED -> R.string.ai_error_safety_blocked
                    AiErrorType.UNKNOWN -> R.string.ai_error_unknown
                },
            )

        // ── Prompt ──────────────────────────────────────────────────────────

        private fun buildSystemInstruction(
            events: List<Event>,
            languageInstruction: String,
        ): String {
            val zone = ZoneId.systemDefault()
            val nowZoned = ZonedDateTime.now(zone)
            val nowStr = dateTimeFormatter.format(nowZoned)
            val nowEpochMillis = nowZoned.toInstant().toEpochMilli()
            val zoneOffset = nowZoned.offset.id

            val groupedEvents =
                events.groupBy {
                    Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate()
                }.toSortedMap()

            val datePrefix = context.getString(R.string.ai_context_date_prefix)
            val allDayLabel = context.getString(R.string.all_day)
            val alarmDisabledLabel = context.getString(R.string.ai_context_alarm_disabled)

            val eventsStr =
                groupedEvents.entries.joinToString("\n\n") { (date, dayEvents) ->
                    "$datePrefix ${date.format(isoDateFormatter)}\n" +
                        dayEvents.joinToString("\n") { event ->
                            val start =
                                Instant.ofEpochMilli(event.startTime)
                                    .atZone(zone)
                                    .toLocalDateTime()
                            val status = if (event.isAlarmEnabled) "" else " $alarmDisabledLabel"
                            val time = if (event.isAllDay) allDayLabel else eventTimeFormatter.format(start)
                            "- $time: ${event.title}$status"
                        }
                }

            return context.getString(
                R.string.ai_system_instruction_template,
                nowStr,
                zoneOffset,
                nowEpochMillis.toString(),
                eventsStr,
                languageInstruction,
            )
        }
    }
