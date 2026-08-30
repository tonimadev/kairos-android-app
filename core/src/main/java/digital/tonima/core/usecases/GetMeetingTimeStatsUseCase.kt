package digital.tonima.core.usecases

import com.google.common.collect.ImmutableList
import digital.tonima.core.model.InsightsPeriod

interface GetMeetingTimeStatsUseCase {
    suspend operator fun invoke(period: InsightsPeriod): ImmutableList<Pair<String, Float>>
}
