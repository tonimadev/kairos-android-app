package digital.tonima.core.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.kairos.core.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@BindType(installIn = BindType.Component.SINGLETON, to = DirectionsRepository::class)
@Singleton
class DirectionsRepositoryImpl
    @Inject
    constructor() : DirectionsRepository {
        private val json = Json { ignoreUnknownKeys = true }

        private val okHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    },
                )
                .build()

        private val retrofit =
            Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

        private val service = retrofit.create(DistanceMatrixService::class.java)

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
            } catch (e: Exception) {
                null
            }
        }
    }
