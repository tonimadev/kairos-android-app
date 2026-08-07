package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBannerView(
    modifier: Modifier = Modifier,
    adId: String,
    isProUser: Boolean,
    loadAd: Boolean = true,
) {
    if (isProUser) return

    val isInspectionMode = LocalInspectionMode.current
    val adWidth = LocalWindowInfo.current.containerSize.width

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth))
                adUnitId = adId

                if (!isInspectionMode && loadAd) {
                    loadAd(AdRequest.Builder().build())
                }
            }
        },
        onRelease = { adView ->
            adView.destroy()
        },
    )
}

@Preview(showBackground = true)
@Composable
fun AdBannerViewPreview() {
    AdBannerView(
        adId = "ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyyyyyy",
        isProUser = false,
        loadAd = false,
    )
}
