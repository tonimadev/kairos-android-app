package digital.tonima.core.repository

interface AuthRepository {
    suspend fun getAccessToken(): String?

    fun getSignInIntent(): android.content.Intent

    suspend fun handleSignInResult(intent: android.content.Intent?): Result<Unit>

    suspend fun signOut()

    fun isSignedIn(): Boolean
}
