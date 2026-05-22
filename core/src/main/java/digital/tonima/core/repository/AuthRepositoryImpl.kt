package digital.tonima.core.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import logcat.logcat
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = AuthRepository::class)
class AuthRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AuthRepository {
        private val webClientId = digital.tonima.kairos.core.BuildConfig.WEB_CLIENT_ID

        // Escopos necessários para a API do Meet v2
        private val scopeMeetCreated = Scope("https://www.googleapis.com/auth/meetings.space.created")
        private val scopeMeetReadonly = Scope("https://www.googleapis.com/auth/meetings.space.readonly")

        private val googleSignInClient: GoogleSignInClient by lazy {
            val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(scopeMeetCreated, scopeMeetReadonly)
                    .requestIdToken(webClientId)
                    .requestServerAuthCode(webClientId, false)
                    .build()
            GoogleSignIn.getClient(context, gso)
        }

        override suspend fun getAccessToken(): String? =
            withContext(Dispatchers.IO) {
                try {
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account != null && account.account != null) {
                        // Obtendo o token de acesso (OAuth2) via GoogleAuthUtil
                        val scopes =
                            "oauth2:https://www.googleapis.com/auth/meetings.space.created " +
                                "https://www.googleapis.com/auth/meetings.space.readonly"
                        return@withContext GoogleAuthUtil.getToken(context, account.account!!, scopes)
                    }
                } catch (e: Exception) {
                    logcat { "getAccessToken failed: ${e.message}" }
                }
                null
            }

        override fun getSignInIntent(): Intent {
            return googleSignInClient.signInIntent
        }

        override suspend fun handleSignInResult(intent: Intent?): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                    val account = task.await()
                    if (account != null) {
                        logcat { "GoogleSignIn success. Email: ${account.email}" }
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Account is null"))
                    }
                } catch (e: Exception) {
                    logcat { "handleSignInResult failed: ${e.message}" }
                    Result.failure(e)
                }
            }

        override suspend fun signOut(): Unit =
            withContext(Dispatchers.IO) {
                try {
                    googleSignInClient.signOut().await()
                } catch (e: Exception) {
                    logcat { "signOut failed: ${e.message}" }
                }
                Unit
            }

        override fun isSignedIn(): Boolean {
            return GoogleSignIn.getLastSignedInAccount(context) != null
        }
    }
