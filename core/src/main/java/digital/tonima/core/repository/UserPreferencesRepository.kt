package digital.tonima.core.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getPreferredCity(): Flow<String?>

    suspend fun setPreferredCity(city: String)

    fun isLocationAlarmEnabled(): Flow<Boolean>

    suspend fun setLocationAlarmEnabled(enabled: Boolean)

    fun getPreferredTransportMode(): Flow<String>

    suspend fun setPreferredTransportMode(mode: String)

    fun isTemperatureInCelsius(): Flow<Boolean>

    suspend fun setTemperatureInCelsius(isCelsius: Boolean)

    fun isProUser(): Flow<Boolean>

    suspend fun setProUser(isPro: Boolean)

    fun isAiUser(): Flow<Boolean>

    suspend fun setAiUser(isAi: Boolean)
}
