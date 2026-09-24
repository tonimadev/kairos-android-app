package digital.tonima.core.analytics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Records handled-but-unexpected failures so they are visible in production, where
 * `logcat` output is not collected (the logger is only installed on debuggable builds).
 */
interface CrashReporter {
    fun recordNonFatal(
        throwable: Throwable,
        message: String,
    )
}

/**
 * Handler for fire-and-forget coroutines launched in custom scopes (receivers, services,
 * singletons): an exception there would otherwise crash the process.
 */
fun CrashReporter.coroutineExceptionHandler(origin: String): CoroutineExceptionHandler =
    CoroutineExceptionHandler { _, throwable ->
        recordNonFatal(throwable, "$origin: uncaught coroutine failure")
    }

/**
 * Runs [block] and, if it fails unexpectedly, reports the failure as a non-fatal and returns
 * [fallback]. Coroutine cancellation is always propagated, never treated as a failure.
 */
inline fun <T> CrashReporter.runOrReport(
    message: String,
    fallback: T,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        recordNonFatal(e, message)
        fallback
    }
