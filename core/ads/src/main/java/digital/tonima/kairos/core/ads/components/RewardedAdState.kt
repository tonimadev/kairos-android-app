package digital.tonima.kairos.core.ads.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import digital.tonima.kairos.core.ads.consent.AdsConsent
import logcat.LogPriority
import logcat.logcat

enum class RewardedAdStatus { Disabled, Loading, Ready, Unavailable }

/**
 * A rewarded video the user opts into. Loads while [rememberRewardedAd] is enabled; [show] plays it
 * and runs `onReward` only if the user watched enough to earn the reward.
 */
@Stable
class RewardedAdState internal constructor(
    private val context: Context,
    private val adUnitId: String,
) {
    var status by mutableStateOf(RewardedAdStatus.Disabled)
        private set

    private var rewardedAd: RewardedAd? = null
    private var isShowing = false

    internal fun load() {
        if (status != RewardedAdStatus.Disabled) return
        status = RewardedAdStatus.Loading
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    status = RewardedAdStatus.Ready
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logcat(LogPriority.WARN) { "Rewarded ad failed to load: ${error.message}" }
                    rewardedAd = null
                    status = RewardedAdStatus.Unavailable
                }
            },
        )
    }

    internal fun release() {
        rewardedAd?.fullScreenContentCallback = null
        rewardedAd = null
        if (!isShowing) status = RewardedAdStatus.Disabled
    }

    fun show(onReward: () -> Unit) {
        val ad = rewardedAd
        val activity = context.findActivity()
        if (ad == null || activity == null || status != RewardedAdStatus.Ready) return
        isShowing = true
        // A rewarded ad is single-use: whatever happens next, a new one is loaded on demand.
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() = finishShowing()

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    logcat(LogPriority.WARN) { "Rewarded ad failed to show: ${error.message}" }
                    finishShowing()
                }
            }
        ad.show(activity) { onReward() }
    }

    private fun finishShowing() {
        isShowing = false
        rewardedAd = null
        status = RewardedAdStatus.Disabled
    }
}

/**
 * Remembers a [RewardedAdState] and keeps one rewarded ad loaded while [enabled] and the user's
 * consent allows ad requests. Nothing is requested for previews or when [enabled] is false.
 */
@Composable
fun rememberRewardedAd(
    adUnitId: String,
    enabled: Boolean,
): RewardedAdState {
    val context = LocalContext.current
    val state = remember(context, adUnitId) { RewardedAdState(context, adUnitId) }
    val canRequestAds by AdsConsent.canRequestAds.collectAsState()
    val shouldLoad = enabled && canRequestAds && !LocalInspectionMode.current

    // Re-keyed on status so a fresh ad is requested after the previous (single-use) one was shown.
    LaunchedEffect(state, shouldLoad, state.status) {
        if (shouldLoad) state.load()
    }
    DisposableEffect(state) { onDispose { state.release() } }
    return state
}

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
