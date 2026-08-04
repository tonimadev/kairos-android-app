package digital.tonima.core.ai.appfunctions

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import digital.tonima.core.repository.CalendarRepository
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.GenerateDailyBriefingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Funções do Kairos expostas para o sistema Android.
 */
class KairosAppFunctions
    @Inject
    constructor(
        private val createEventUseCase: CreateEventUseCase,
        private val generateDailyBriefingUseCase: GenerateDailyBriefingUseCase,
        private val calendarRepository: CalendarRepository,
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

                createEventUseCase(
                    calendarId = calendarId,
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
                    calendarRepository.getEventsForMonth(YearMonth.from(today))
                        .filter {
                            val eventDate =
                                java.time.Instant.ofEpochMilli(it.startTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            eventDate == today
                        }

                generateDailyBriefingUseCase(
                    events = events,
                    languageInstruction = "Responda em Português",
                    wakeUpTime = null,
                    city = null,
                ) ?: "Não foi possível gerar o resumo no momento."
            }
    }
