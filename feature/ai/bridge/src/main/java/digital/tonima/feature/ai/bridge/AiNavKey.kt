package digital.tonima.feature.ai.bridge

import digital.tonima.kairos.core.navigation.AppNavigator
import digital.tonima.kairos.core.navigation.FeatureNavKey

/** Destinations owned by the AI feature, reachable through [AppNavigator]. */
sealed interface AiNavKey : FeatureNavKey {
    data object ChatHistory : AiNavKey

    data class ChatDetail(val conversationId: Long) : AiNavKey
}
