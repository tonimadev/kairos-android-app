package digital.tonima.core.ai

/**
 * Defines the risk level of an AI tool action.
 *
 * - [SAFE]: Executes immediately without any user feedback.
 * - [MODERATE]: Executes immediately but shows a snackbar notification.
 * - [CRITICAL]: Requires explicit user confirmation before execution.
 */
enum class RiskLevel {
    SAFE,
    MODERATE,
    CRITICAL,
}
