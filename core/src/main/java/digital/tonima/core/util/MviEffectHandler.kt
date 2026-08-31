package digital.tonima.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> MviEffectHandler(
    effectFlow: Flow<T?>,
    onConsume: () -> Unit,
    onEffect: (T) -> Unit,
) {
    val currentOnEffect by rememberUpdatedState(onEffect)
    val currentOnConsume by rememberUpdatedState(onConsume)

    LaunchedEffect(effectFlow) {
        effectFlow.collect { effect ->
            if (effect != null) {
                currentOnEffect(effect)
                currentOnConsume()
            }
        }
    }
}
