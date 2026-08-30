package digital.tonima.core.viewmodel

import android.content.Intent

sealed class AuthIntent : BaseIntent {
    data object ConsumeEffect : AuthIntent()

    object SignInWithGoogle : AuthIntent()

    object SignOutFromGoogle : AuthIntent()

    data class HandleGoogleSignInResult(val resultData: Intent?) : AuthIntent()
}
