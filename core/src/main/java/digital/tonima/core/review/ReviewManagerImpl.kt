package digital.tonima.core.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewManagerFactory
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = ReviewManager::class)
class ReviewManagerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ReviewManager {
        override fun requestReview(
            activity: Activity,
            onComplete: () -> Unit,
        ) {
            val manager = ReviewManagerFactory.create(context)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        onComplete()
                    }
                } else {
                    // Fallback para a Play Store se a API falhar
                    val intent = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    onComplete()
                }
            }
        }
    }
