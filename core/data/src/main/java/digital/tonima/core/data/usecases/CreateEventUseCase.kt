package digital.tonima.core.data.usecases

interface CreateEventUseCase {
    suspend operator fun invoke(
        calendarId: Long,
        title: String,
        description: String? = null,
        location: String? = null,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean = false,
    ): Long?
}
