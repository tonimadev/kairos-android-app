package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = adId
                if (loadAd) {
                    loadAd(AdRequest.Builder().build())
                }
            }
        },
        update = { adView ->
            if (adView.adUnitId != adId) {
                adView.adUnitId = adId
                if (loadAd) {
                    adView.loadAd(AdRequest.Builder().build())
                }
            }
        },
    )
}
