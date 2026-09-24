package digital.tonima.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.paulrybitskyi.hiltbinder.BindType
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = CrashReporter::class)
class FirebaseCrashReporter
    @Inject
    constructor() : CrashReporter {
        override fun recordNonFatal(
            throwable: Throwable,
            message: String,
        ) {
            logcat(LogPriority.ERROR) { "$message\n${throwable.asLog()}" }
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log(message)
            crashlytics.recordException(throwable)
        }
    }
