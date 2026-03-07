package digital.tonima.core.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = Analytics::class)
class FirebaseAnalyticsImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Analytics {
        private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        override fun logEvent(
            name: String,
            params: Map<String, Any?>,
        ) {
            firebaseAnalytics.logEvent(name) {
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> param(key, value)
                        is Int -> param(key, value.toLong())
                        is Long -> param(key, value)
                        is Double -> param(key, value)
                        is Boolean -> param(key, if (value) 1L else 0L)
                        else -> param(key, value.toString())
                    }
                }
            }
        }
    }
