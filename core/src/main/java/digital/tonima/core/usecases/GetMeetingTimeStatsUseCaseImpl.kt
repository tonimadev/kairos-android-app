package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.model.InsightsPeriod
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.CalendarRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = GetMeetingTimeStatsUseCase::class)
class GetMeetingTimeStatsUseCaseImpl
    @Inject
    constructor(
        private val calendarRepository: CalendarRepository,
        private val appPreferencesRepository: AppPreferencesRepository,
    ) : GetMeetingTimeStatsUseCase {
        override suspend operator fun invoke(period: InsightsPeriod): List<Pair<String, Float>> {
            val enabledCalendarIdStrings = appPreferencesRepository.getEnabledCalendarIds().firstOrNull() ?: emptySet()
            val allowedCalendarIds = enabledCalendarIdStrings.mapNotNull { it.toLongOrNull() }

            val now = LocalDate.now()
            val result = mutableListOf<Pair<String, Float>>()

            when (period) {
                InsightsPeriod.WEEK -> {
                    // Fetch events for current month (covers most of the week)
                    val events = calendarRepository.getEventsForMonth(YearMonth.from(now), allowedCalendarIds)

                    val startOfWeek = now.minusDays(now.dayOfWeek.value.toLong() - 1) // Monday
                    val formatter = DateTimeFormatter.ofPattern("EEE")

                    for (i in 0..6) {
                        val day = startOfWeek.plusDays(i.toLong())

                        val dayEvents =
                            events.filter { event ->
                                val eventDate =
                                    Instant.ofEpochMilli(event.startTime)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                eventDate == day && event.durationMinutes > 0 && event.hasMeetingUrl
                            }

                        val totalMinutes = dayEvents.sumOf { it.durationMinutes }
                        val hours = totalMinutes / 60f
                        result.add(Pair(day.format(formatter), hours))
                    }
                }
                InsightsPeriod.DAY -> {
                    val events = calendarRepository.getEventsForMonth(YearMonth.from(now), allowedCalendarIds)

                    val formatter = DateTimeFormatter.ofPattern("HH:mm")

                    // Break day into morning, afternoon, evening
                    val dayEvents =
                        events.filter { event ->
                            val eventDate =
                                Instant.ofEpochMilli(event.startTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            eventDate == now && event.durationMinutes > 0 && event.hasMeetingUrl
                        }

                    var morning = 0f
                    var afternoon = 0f
                    var evening = 0f

                    dayEvents.forEach { event ->
                        val time = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault())
                        when (time.hour) {
                            in 0..11 -> morning += (event.durationMinutes / 60f)
                            in 12..17 -> afternoon += (event.durationMinutes / 60f)
                            else -> evening += (event.durationMinutes / 60f)
                        }
                    }
                    result.add(Pair("Morning", morning))
                    result.add(Pair("Afternoon", afternoon))
                    result.add(Pair("Evening", evening))
                }
                InsightsPeriod.MONTH -> {
                    val events = calendarRepository.getEventsForMonth(YearMonth.from(now), allowedCalendarIds)

                    var week1 = 0f
                    var week2 = 0f
                    var week3 = 0f
                    var week4 = 0f

                    val monthEvents =
                        events.filter { event ->
                            val eventDate =
                                Instant.ofEpochMilli(event.startTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            eventDate.month == now.month && event.durationMinutes > 0 && event.hasMeetingUrl
                        }

                    monthEvents.forEach { event ->
                        val date =
                            Instant.ofEpochMilli(event.startTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        when (date.dayOfMonth) {
                            in 1..7 -> week1 += (event.durationMinutes / 60f)
                            in 8..14 -> week2 += (event.durationMinutes / 60f)
                            in 15..21 -> week3 += (event.durationMinutes / 60f)
                            else -> week4 += (event.durationMinutes / 60f)
                        }
                    }

                    result.add(Pair("W1", week1))
                    result.add(Pair("W2", week2))
                    result.add(Pair("W3", week3))
                    result.add(Pair("W4+", week4))
                }
            }

            return result
        }
    }
