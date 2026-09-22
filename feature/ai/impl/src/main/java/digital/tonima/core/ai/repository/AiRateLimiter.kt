package digital.tonima.core.ai.repository

import digital.tonima.core.ai.AIConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory sliding-window limiter guarding LLM calls against runaway cost/abuse
 * (e.g. a stuck UI retry loop or a user spamming send). Not a substitute for
 * server-side quota enforcement, just a cheap client-side circuit breaker.
 */
@Singleton
class AiRateLimiter
    @Inject
    constructor() {
        private val callTimestamps = ArrayDeque<Long>()

        @Synchronized
        fun tryAcquire(
            maxCallsPerWindow: Int = AIConfig.MAX_REQUESTS_PER_MINUTE,
            windowMs: Long = WINDOW_MS,
            now: Long = System.currentTimeMillis(),
        ): Boolean {
            while (callTimestamps.isNotEmpty() && now - callTimestamps.first() > windowMs) {
                callTimestamps.removeFirst()
            }
            if (callTimestamps.size >= maxCallsPerWindow) {
                return false
            }
            callTimestamps.addLast(now)
            return true
        }

        private companion object {
            const val WINDOW_MS = 60_000L
        }
    }
