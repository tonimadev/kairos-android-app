package digital.tonima.core.usecases

interface CreateEventUseCase {
    suspend fun invoke(
        calendarId: Long,
        title: String,
        description: String? = null,
        location: String? = null,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean = false,
    ): Long?
}
