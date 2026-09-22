package digital.tonima.core.ai.usecases

import android.content.Context
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.repository.AiModelRepository
import digital.tonima.core.ai.repository.AiModelResult
import digital.tonima.core.ai.repository.DailyBriefingRepository
import digital.tonima.core.data.repository.WeatherRepository
import digital.tonima.core.util.toOpenWeatherLang
import digital.tonima.core.viewmodel.UiText
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.Event
import digital.tonima.kairos.core.model.Weather
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = GenerateDailyBriefingUseCase::class)
class GenerateDailyBriefingUseCaseImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val weatherRepository: WeatherRepository,
        private val dailyBriefingRepository: DailyBriefingRepository,
        private val aiModelRepository: AiModelRepository,
    ) : GenerateDailyBriefingUseCase {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        override suspend fun invoke(
            events: List<Event>,
            languageInstruction: String,
            wakeUpTime: String?,
            city: String?,
        ): BriefingResult {
            val today = LocalDate.now()
            if (dailyBriefingRepository.getLastGeneratedDate() == today) {
                dailyBriefingRepository.getDailyBriefing().first()?.let { cached ->
                    return BriefingResult.Cached(cached)
                }
            }

            val lang = Locale.getDefault().toOpenWeatherLang()
            val weather = if (city != null) weatherRepository.getWeather(city, lang = lang) else null
            val prompt = buildPrompt(events, languageInstruction, wakeUpTime, weather)

            return when (val result = aiModelRepository.generateBriefingContent(prompt)) {
                is AiModelResult.Text -> {
                    dailyBriefingRepository.saveDailyBriefing(result.text, today)
                    BriefingResult.Success(result.text)
                }
                is AiModelResult.Error -> BriefingResult.Error(result.type.toUiText(), result.type)
                is AiModelResult.FunctionCall -> {
                    // Briefing generation never registers tools, so this cannot happen in practice.
                    BriefingResult.Error(AiErrorType.UNKNOWN.toUiText(), AiErrorType.UNKNOWN)
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

        private fun buildPrompt(
            events: List<Event>,
            languageInstruction: String,
            wakeUpTime: String?,
            weather: Weather?,
        ): String {
            val eventsStr =
                if (events.isEmpty()) {
                    context.getString(R.string.ai_context_no_events_today)
                } else {
                    events.joinToString("\n") { event ->
                        val time =
                            if (event.isAllDay) {
                                context.getString(R.string.all_day)
                            } else {
                                val localTime =
                                    Instant.ofEpochMilli(event.startTime)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalTime()
                                timeFormatter.format(localTime)
                            }
                        "- $time: ${event.title}"
                    }
                }

            val wakeUpContext =
                if (wakeUpTime != null) {
                    context.getString(R.string.ai_context_wake_up, wakeUpTime)
                } else {
                    ""
                }

            val weatherContext =
                if (weather != null) {
                    context.getString(
                        R.string.ai_context_weather,
                        weather.city,
                        weather.temperature.toInt(),
                        weather.description,
                    )
                } else {
                    ""
                }

            return context.getString(
                R.string.ai_daily_briefing_prompt_template,
                wakeUpContext,
                weatherContext,
                languageInstruction,
                eventsStr,
            )
        }
    }
