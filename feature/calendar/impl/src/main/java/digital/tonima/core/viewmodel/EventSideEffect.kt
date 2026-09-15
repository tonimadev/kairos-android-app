package digital.tonima.core.viewmodel

sealed class EventSideEffect {
    data class ShowSnackbar(val message: UiText) : EventSideEffect()

    data class OpenMeetingUrl(val url: String) : EventSideEffect()

    data class CopyToClipboard(val text: String, val message: UiText) : EventSideEffect()

    data object RequestAppReview : EventSideEffect()

    data object RequestSubscription : EventSideEffect()

    data object RequestPurchase : EventSideEffect()

    data class AIToolError(val message: UiText) : EventSideEffect()
}
