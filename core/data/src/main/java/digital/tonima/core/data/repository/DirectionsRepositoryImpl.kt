package digital.tonima.core.data.repository

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.kairos.core.BuildConfig
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

@BindType(installIn = BindType.Component.SINGLETON, to = DirectionsRepository::class)
@Singleton
class DirectionsRepositoryImpl
    @Inject
    constructor(
        private val service: DistanceMatrixService,
    ) : DirectionsRepository {
        override suspend fun getTravelTimeSeconds(
            origin: String,
            destination: String,
            mode: String,
        ): Int? {
            if (BuildConfig.GOOGLE_MAPS_API_KEY.isEmpty()) return null
            return try {
                val response = service.getDistanceMatrix(origin, destination, mode, BuildConfig.GOOGLE_MAPS_API_KEY)
                if (response.status == "OK") {
                    response.rows.firstOrNull()?.elements?.firstOrNull()?.let { element ->
                        if (element.status == "OK") {
                            element.duration?.value
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { "Directions request failed: ${e.message}" }
                null
            }
        }
    }
