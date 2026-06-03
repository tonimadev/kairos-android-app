package digital.tonima.core.usecases

import digital.tonima.core.repository.CalendarRepository
import digital.tonima.core.utils.IcsParser
import javax.inject.Inject

class ImportIcsUseCase
    @Inject
    constructor(
        private val calendarRepository: CalendarRepository,
        private val toggleEventAlarmUseCase: ToggleEventAlarmUseCase,
    ) {
        suspend operator fun invoke(
            content: String,
            calendarName: String,
            color: Int,
            alarmsEnabled: Boolean,
        ): Result<Unit> {
            return try {
                val events = IcsParser.parseIcs(content)
                if (events.isEmpty()) return Result.failure(Exception("Nenhum evento encontrado no arquivo ICS"))

                val calendarId =
                    calendarRepository.createLocalCalendar(calendarName, color)
                        ?: return Result.failure(
                            Exception("Falha ao criar o calendário local. Verifique as permissões de Calendário."),
                        )

                for (event in events) {
                    val insertedId =
                        calendarRepository.insertEvent(
                            calendarId = calendarId,
                            title = event.title,
                            description = null,
                            location = event.location,
                            startTime = event.startTime,
                            endTime = event.endTime,
                            isAllDay = event.isAllDay,
                        )

                    if (insertedId != null && !alarmsEnabled) {
                        val savedEvent = event.copy(id = insertedId)
                        toggleEventAlarmUseCase.invoke(
                            event = savedEvent,
                            isEnabled = false,
                            disableAllOccurrences = true,
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
