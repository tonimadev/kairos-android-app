package digital.tonima.core.review

import android.app.Activity

interface ReviewManager {
    fun requestReview(
        activity: Activity,
        onComplete: () -> Unit,
    )
}
