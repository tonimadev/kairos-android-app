package digital.tonima.kairos.core.ads.components

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import digital.tonima.kairos.core.ads.consent.AdsConsent
import logcat.LogPriority
import logcat.logcat

/**
 * Loads up to [count] native ads while [enabled] and consent allows ad requests, and destroys them
 * when the caller leaves composition. Hoist this above any lazy list: a list item that loads its own
 * ad would re-request one every time it scrolls back into view.
 */
@Composable
fun rememberNativeAds(
    adUnitId: String,
    count: Int,
    enabled: Boolean,
): List<NativeAd> {
    val context = LocalContext.current
    val canRequestAds by AdsConsent.canRequestAds.collectAsState()
    val shouldLoad = enabled && canRequestAds && count > 0 && !LocalInspectionMode.current
    val ads = remember(adUnitId) { mutableStateListOf<NativeAd>() }

    DisposableEffect(adUnitId, count, shouldLoad) {
        var disposed = false
        if (shouldLoad) {
            AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad ->
                    if (disposed) ad.destroy() else ads.add(ad)
                }
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            logcat(LogPriority.WARN) { "Native ad failed to load: ${error.message}" }
                        }
                    },
                )
                .build()
                .loadAds(AdRequest.Builder().build(), count)
        }
        onDispose {
            disposed = true
            ads.forEach { it.destroy() }
            ads.clear()
        }
    }
    return ads
}

/**
 * A native ad styled with the app theme. The "ad" badge is mandatory for native ads, so [adLabel]
 * (the localized "Ad") is always shown.
 */
@Composable
fun NativeAdCard(
    nativeAd: NativeAd,
    adLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val style =
        NativeAdStyle(
            background = colors.surfaceVariant.toArgb(),
            title = colors.onSurface.toArgb(),
            body = colors.onSurfaceVariant.toArgb(),
            accent = colors.primary.toArgb(),
            onAccent = colors.onPrimary.toArgb(),
        )
    // Keyed on the ad: a NativeAdView is bound to one ad for its lifetime, and rebinding on every
    // recomposition would re-register the same ad.
    key(nativeAd) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                NativeAdViews.create(context, style, adLabel).also { NativeAdViews.bind(it, nativeAd) }
            },
        )
    }
}

internal data class NativeAdStyle(
    val background: Int,
    val title: Int,
    val body: Int,
    val accent: Int,
    val onAccent: Int,
)

/** Builds the [NativeAdView] tree in code so it follows the Compose theme instead of an XML theme. */
private object NativeAdViews {
    private const val CORNER_DP = 16
    private const val PADDING_DP = 16
    private const val ICON_DP = 40
    private const val MEDIA_HEIGHT_DP = 160
    private const val GAP_DP = 8

    fun create(
        context: Context,
        style: NativeAdStyle,
        adLabel: String,
    ): NativeAdView {
        val adView = NativeAdView(context)
        adView.background =
            GradientDrawable().apply {
                setColor(style.background)
                cornerRadius = context.dp(CORNER_DP).toFloat()
            }
        adView.clipToOutline = true

        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        column.setPadding(
            context.dp(PADDING_DP),
            context.dp(PADDING_DP),
            context.dp(PADDING_DP),
            context.dp(PADDING_DP),
        )

        val media = MediaView(context)
        media.layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(MEDIA_HEIGHT_DP))
                .apply { bottomMargin = context.dp(GAP_DP) }
        column.addView(media)

        val icon = ImageView(context)
        icon.layoutParams =
            LinearLayout.LayoutParams(context.dp(ICON_DP), context.dp(ICON_DP)).apply { marginEnd = context.dp(GAP_DP) }

        val headline = text(context, style.title, TITLE_SP, Typeface.BOLD)
        val advertiser = text(context, style.body, CAPTION_SP, Typeface.NORMAL)
        val titles =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(headline)
                addView(advertiser)
            }
        val badge =
            text(context, style.onAccent, CAPTION_SP, Typeface.BOLD).apply {
                text = adLabel
                setPadding(context.dp(6), context.dp(2), context.dp(6), context.dp(2))
                background =
                    GradientDrawable().apply {
                        setColor(style.accent)
                        cornerRadius = context.dp(4).toFloat()
                    }
            }
        val header =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(icon)
                addView(titles)
                addView(badge)
            }
        column.addView(header)

        val body = text(context, style.body, BODY_SP, Typeface.NORMAL)
        body.layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = context.dp(GAP_DP) }
        column.addView(body)

        val cta = Button(context)
        cta.setTextColor(style.onAccent)
        cta.backgroundTintList = android.content.res.ColorStateList.valueOf(style.accent)
        cta.layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = context.dp(GAP_DP) }
        column.addView(cta)

        adView.addView(column, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        adView.mediaView = media
        adView.iconView = icon
        adView.headlineView = headline
        adView.advertiserView = advertiser
        adView.bodyView = body
        adView.callToActionView = cta
        return adView
    }

    fun bind(
        adView: NativeAdView,
        nativeAd: NativeAd,
    ) {
        (adView.headlineView as TextView).text = nativeAd.headline
        adView.mediaView?.apply {
            visibility = if (nativeAd.mediaContent != null) View.VISIBLE else View.GONE
            nativeAd.mediaContent?.let { setMediaContent(it) }
        }
        adView.bind(adView.bodyView as TextView, nativeAd.body)
        adView.bind(adView.advertiserView as TextView, nativeAd.advertiser)
        adView.bind(adView.callToActionView as Button, nativeAd.callToAction)
        (adView.iconView as ImageView).apply {
            val drawable = nativeAd.icon?.drawable
            setImageDrawable(drawable)
            visibility = if (drawable != null) View.VISIBLE else View.GONE
        }
        adView.setNativeAd(nativeAd)
    }

    private fun NativeAdView.bind(
        view: TextView,
        value: String?,
    ) {
        view.text = value
        view.visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun text(
        context: Context,
        color: Int,
        sizeSp: Float,
        typefaceStyle: Int,
    ) = TextView(context).apply {
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTypeface(typeface, typefaceStyle)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun Context.dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private const val TITLE_SP = 16f
    private const val BODY_SP = 14f
    private const val CAPTION_SP = 12f
}
