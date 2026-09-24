package digital.tonima.core.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.permissions.PermissionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    suspend fun getCurrentLocation(): String? // returns "lat,lon"
}

@BindType(installIn = BindType.Component.SINGLETON, to = LocationRepository::class)
@Singleton
class LocationRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val permissionManager: PermissionManager,
    ) : LocationRepository {
        private val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        @SuppressLint("MissingPermission")
        override suspend fun getCurrentLocation(): String? {
            if (!permissionManager.hasLocationPermission()) return null

            return try {
                val location = fusedLocationClient.lastLocation.await()
                location?.let { "${it.latitude},${it.longitude}" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Location services unavailable or permission revoked meanwhile.
                logcat(LogPriority.WARN) { "Could not read the current location: ${e.message}" }
                null
            }
        }
    }
