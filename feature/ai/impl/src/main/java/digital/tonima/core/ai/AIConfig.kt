package digital.tonima.core.ai

object AIConfig {
    const val GEMINI_MODEL = "gemini-3.5-flash-lite"
    const val GEMINI_NANO_MODEL = "gemini-nano"

    /** Client-side cap to protect against runaway cost/abuse; independent of any server-side quota. */
    const val MAX_REQUESTS_PER_MINUTE = 10

    /** Retries only apply to [digital.tonima.core.ai.repository.AiErrorType.NETWORK] failures. */
    const val RETRY_MAX_ATTEMPTS = 2
    const val RETRY_BASE_DELAY_MS = 500L
}
