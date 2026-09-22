package digital.tonima.core.ai.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRateLimiterTest {
    private val limiter = AiRateLimiter()

    @Test
    fun `allows calls up to the window limit`() {
        repeat(3) {
            assertTrue(limiter.tryAcquire(maxCallsPerWindow = 3, windowMs = 60_000, now = 1_000L))
        }
    }

    @Test
    fun `blocks calls once the window limit is reached`() {
        repeat(3) {
            limiter.tryAcquire(maxCallsPerWindow = 3, windowMs = 60_000, now = 1_000L)
        }

        assertFalse(limiter.tryAcquire(maxCallsPerWindow = 3, windowMs = 60_000, now = 1_000L))
    }

    @Test
    fun `allows calls again once the window has elapsed`() {
        repeat(3) {
            limiter.tryAcquire(maxCallsPerWindow = 3, windowMs = 60_000, now = 1_000L)
        }
        assertFalse(limiter.tryAcquire(maxCallsPerWindow = 3, windowMs = 60_000, now = 1_000L))

        val afterWindow = 1_000L + 60_001L
        assertTrue(limiter.tryAcquire(maxCallsPerWindow = 3, windowMs = 60_000, now = afterWindow))
    }
}
