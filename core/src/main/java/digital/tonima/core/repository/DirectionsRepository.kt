package digital.tonima.core.repository

import digital.tonima.core.model.DistanceMatrixResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsRepository {
    suspend fun getTravelTimeSeconds(
        origin: String,
        destination: String,
        mode: String,
    ): Int?
}

interface DistanceMatrixService {
    @GET("distancematrix/json")
    suspend fun getDistanceMatrix(
        @Query("origins") origins: String,
        @Query("destinations") destinations: String,
        @Query("mode") mode: String,
        @Query("key") apiKey: String,
        @Query("departure_time") departureTime: String = "now",
    ): DistanceMatrixResponse
}
