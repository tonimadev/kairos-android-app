package digital.tonima.core.ai.appfunctions

import android.content.Context
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.ai.usecases.BriefingResult
import digital.tonima.core.ai.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.data.usecases.CreateEventUseCase
import digital.tonima.core.data.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.data.usecases.GetEventsForMonthUseCase
import digital.tonima.kairos.core.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Funções do Kairos expostas para o sistema Android.
 *
 * `androidx.appfunctions` is still pre-1.0 (1.0.0-alpha11 as of writing) — its API can change
 * between alpha releases. This file is the only surface in the app affected, so a breaking
 * change is contained and easy to patch here.
 */
class KairosAppFunctions
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val createEventUseCase: CreateEventUseCase,
        private val generateDailyBriefingUseCase: GenerateDailyBriefingUseCase,
        private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
        private val getAvailableCalendarsUseCase: GetAvailableCalendarsUseCase,
    ) {
        /**
         * Cria um novo evento no calendário do usuário.
         * Use esta função quando o usuário pedir para agendar, criar ou adicionar um compromisso, reunião ou lembrete.
         *
         * @param context O contexto de execução fornecido pelo sistema.
         * @param title O título ou nome do evento (ex: "Reunião de Time").
         * @param startTime O horário de início em milissegundos desde a época (epoch millis).
         * @param endTime O horário de término em milissegundos desde a época (epoch millis).
         * Se não informado, presume-se que o evento dure 1 hora.
         * @param calendarId O identificador do calendário (use 1 como padrão se não souber).
         * Se esse calendário não existir, o evento é criado no primeiro calendário disponível.
         * @param description Notas ou detalhes adicionais sobre o compromisso.
         * @param location O local onde o evento ocorrerá (presencial ou link).
         * @param isAllDay Se verdadeiro, o evento será marcado para o dia inteiro, ignorando as horas.
         * @return O ID numérico do evento criado no banco de dados, ou -1 se falhar.
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun createEvent(
            context: AppFunctionContext,
            title: String,
            startTime: Long,
            endTime: Long? = null,
            calendarId: Long = 1L,
            description: String? = null,
            location: String? = null,
            isAllDay: Boolean = false,
        ): Long =
            withContext(Dispatchers.IO) {
                val calculatedEndTime = endTime ?: (startTime + 3_600_000L) // +1 hour in millis
                // The model is told to default to 1, which may not exist on this device.
                val calendars = getAvailableCalendarsUseCase()
                val targetCalendarId =
                    calendars.firstOrNull { it.id == calendarId }?.id
                        ?: calendars.firstOrNull()?.id
                        ?: return@withContext -1L

                createEventUseCase(
                    calendarId = targetCalendarId,
                    title = title,
                    description = description,
                    location = location,
                    startTime = startTime,
                    endTime = calculatedEndTime,
                    isAllDay = isAllDay,
                ) ?: -1L
            }

        /**
         * Gera e retorna um resumo inteligente dos eventos e compromissos agendados para o dia de hoje.
         * O resumo inclui informações contextuais sobre o clima e proatividade.
         *
         * @param context O contexto de execução fornecido pelo sistema.
         * @return Um texto amigável com o briefing do dia.
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun getDailyBriefing(context: AppFunctionContext): String =
            withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                val events =
                    getEventsForMonthUseCase(
                        YearMonth.from(today).atDay(1).toEpochDay(),
                    )
                        .filter {
                            val eventDate =
                                java.time.Instant.ofEpochMilli(it.startTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            eventDate == today
                        }

                when (
                    val result =
                        generateDailyBriefingUseCase(
                            events = events,
                            languageInstruction = appContext.getString(R.string.ai_briefing_instruction),
                            wakeUpTime = null,
                            city = null,
                        )
                ) {
                    is BriefingResult.Success -> result.text
                    is BriefingResult.Cached -> result.text
                    is BriefingResult.Error -> result.message.asString(appContext)
                }
            }
    }
