package digital.tonima.core.ai.repository

/**
 * Result of a single interaction with the underlying LLM SDK, free of any
 * Firebase AI types so callers never need to depend on the SDK directly.
 */
sealed class AiModelResult {
    /** Cumulative text produced so far (for streaming, this grows with each emission). */
    data class Text(val text: String) : AiModelResult()

    /** The model decided to invoke a registered tool / function. */
    data class FunctionCall(val name: String, val args: Map<String, Any?>) : AiModelResult()

    data class Error(val type: AiErrorType, val cause: Throwable) : AiModelResult()
}

enum class AiErrorType {
    /** Transient/connectivity failure — safe to retry. */
    NETWORK,

    /** Quota/usage limit hit. */
    RATE_LIMITED,

    /** Prompt or response was blocked/stopped by safety filtering. */
    SAFETY_BLOCKED,

    /** Config/auth/programmer error, or anything not otherwise classified. */
    UNKNOWN,
}
