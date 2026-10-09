package digital.tonima.kairos.core.ads.consent

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import logcat.logcat

/** What the Google UMP SDK knows, behind a seam so the consent flow can be tested without it. */
internal interface ConsentSource {
    fun canRequestAds(): Boolean

    fun isPrivacyOptionsRequired(): Boolean

    /** Refreshes consent info and shows the consent form when needed; [onDone] gets an error message or null. */
    fun gather(
        activity: Activity,
        onDone: (errorMessage: String?) -> Unit,
    )

    fun showPrivacyOptions(
        activity: Activity,
        onDone: (errorMessage: String?) -> Unit,
    )
}

internal class UmpConsentSource(context: Context) : ConsentSource {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    override fun canRequestAds(): Boolean = consentInformation.canRequestAds()

    override fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun gather(
        activity: Activity,
        onDone: (errorMessage: String?) -> Unit,
    ) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    onDone(formError?.message)
                }
            },
            { requestError -> onDone(requestError.message) },
        )
    }

    override fun showPrivacyOptions(
        activity: Activity,
        onDone: (errorMessage: String?) -> Unit,
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError -> onDone(formError?.message) }
    }
}

/**
 * Tracks whether the user's consent (GDPR / UK / US-state rules, via Google's UMP) allows requesting
 * ads. Ad requests and `MobileAds.initialize` must wait for [canRequestAds]; when consent cannot be
 * gathered (offline, form failure) it stays at whatever UMP cached from the last successful run, so
 * a user who already consented keeps seeing ads and a user who never did is never tracked.
 */
class AdsConsentManager internal constructor(private val source: ConsentSource) {
    private val _canRequestAds = MutableStateFlow(source.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(source.isPrivacyOptionsRequired())
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    /** Call from the main activity's `onCreate`: shows the consent form on first launch / when it changed. */
    fun gather(activity: Activity) {
        source.gather(activity) { error ->
            if (error != null) {
                logcat(LogPriority.WARN) { "Ads consent could not be gathered, keeping cached state: $error" }
            }
            publish()
        }
    }

    /** Re-opens the consent form so the user can change their choice (a settings entry, when required). */
    fun showPrivacyOptions(activity: Activity) {
        source.showPrivacyOptions(activity) { error ->
            if (error != null) {
                logcat(LogPriority.WARN) { "Ads privacy options form failed: $error" }
            }
            publish()
        }
    }

    private fun publish() {
        _canRequestAds.value = source.canRequestAds()
        _isPrivacyOptionsRequired.value = source.isPrivacyOptionsRequired()
    }
}

/** Process-wide access to [AdsConsentManager]; [install] runs once from `Application.onCreate`. */
object AdsConsent {
    private val denied = MutableStateFlow(false).asStateFlow()

    @Volatile
    private var manager: AdsConsentManager? = null

    val canRequestAds: StateFlow<Boolean> get() = manager?.canRequestAds ?: denied

    val isPrivacyOptionsRequired: StateFlow<Boolean> get() = manager?.isPrivacyOptionsRequired ?: denied

    fun install(context: Context) {
        if (manager == null) {
            manager = AdsConsentManager(UmpConsentSource(context))
        }
    }

    fun gather(activity: Activity) {
        manager?.gather(activity)
    }

    fun showPrivacyOptions(activity: Activity) {
        manager?.showPrivacyOptions(activity)
    }
}
