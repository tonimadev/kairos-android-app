package digital.tonima.core.analytics

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Test

class CrashReporterTest {
    private val crashReporter: CrashReporter = mockk(relaxed = true)

    @Test
    fun `runOrReport returns the block result when it succeeds`() {
        assertEquals(1, crashReporter.runOrReport("msg", fallback = 0) { 1 })
        verify(exactly = 0) { crashReporter.recordNonFatal(any(), any()) }
    }

    @Test
    fun `runOrReport reports a failure and returns the fallback`() {
        val failure = IllegalStateException("boom")

        val result = crashReporter.runOrReport("reading prefs", fallback = 0) { throw failure }

        assertEquals(0, result)
        verify { crashReporter.recordNonFatal(failure, "reading prefs") }
    }

    @Test(expected = CancellationException::class)
    fun `runOrReport never swallows coroutine cancellation`() {
        crashReporter.runOrReport("msg", fallback = 0) { throw CancellationException("cancelled") }
    }
}
