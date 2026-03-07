@file:Suppress("ktlint:standard:max-line-length")

package digital.tonima.kairos.wear.ui.actions

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.CATEGORY_BROWSABLE
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import logcat.LogPriority
import logcat.logcat

object OpenOnPhone {
    fun launch(context: Context) {
        val helper = RemoteActivityHelper(context)

        val deepLink = "digital.tonima.kairos://open".toUri()
        val openIntent =
            Intent(ACTION_VIEW, deepLink)
                .addCategory(CATEGORY_BROWSABLE)
                .addCategory(Intent.CATEGORY_DEFAULT)

        val future = helper.startRemoteActivity(openIntent)
        Futures.addCallback(
            future,
            object : FutureCallback<Void?> {
                override fun onSuccess(result: Void?) {
                    logcat(tag = "WearApp") { "OpenOnPhone: launched on phone successfully." }
                }

                override fun onFailure(t: Throwable) {
                    logcat(tag = "WearApp", priority = LogPriority.ERROR) {
                        "OpenOnPhone failed to launch app on phone: ${t.localizedMessage}"
                    }
                    val marketIntent =
                        Intent(
                            ACTION_VIEW,
                            "market://details?id=digital.tonima.kairos".toUri(),
                        ).addCategory(CATEGORY_BROWSABLE)

                    val marketFuture = helper.startRemoteActivity(marketIntent)
                    Futures.addCallback(
                        marketFuture,
                        object : FutureCallback<Void?> {
                            override fun onSuccess(result: Void?) {
                                logcat(tag = "WearApp") { "OpenOnPhone: opened Play Store on phone." }
                            }

                            override fun onFailure(t2: Throwable) {
                                logcat(tag = "WearApp", priority = LogPriority.ERROR) {
                                    "OpenOnPhone failed to open Play Store on phone: " +
                                        "${t2.localizedMessage}. Trying web URL…"
                                }

                                val webIntent =
                                    Intent(
                                        ACTION_VIEW,
                                        "https://play.google.com/store/apps/details?id=digital.tonima.kairos".toUri(),
                                    ).addCategory(CATEGORY_BROWSABLE)

                                val webFuture = helper.startRemoteActivity(webIntent)
                                Futures.addCallback(
                                    webFuture,
                                    object : FutureCallback<Void?> {
                                        override fun onSuccess(result: Void?) {
                                            logcat(
                                                tag = "WearApp",
                                            ) { "OpenOnPhone: opened Play Store web URL on phone." }
                                        }

                                        override fun onFailure(t3: Throwable) {
                                            logcat(tag = "WearApp", priority = LogPriority.ERROR) {
                                                "OpenOnPhone fallback to web URL also failed: ${t3.localizedMessage}"
                                            }
                                        }
                                    },
                                    MoreExecutors.directExecutor(),
                                )
                            }
                        },
                        MoreExecutors.directExecutor(),
                    )
                }
            },
            MoreExecutors.directExecutor(),
        )
    }
}
