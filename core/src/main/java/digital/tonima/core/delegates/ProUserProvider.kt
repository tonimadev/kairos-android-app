package digital.tonima.core.delegates

import kotlinx.coroutines.flow.StateFlow

interface ProUserProvider {
    val isProUser: StateFlow<Boolean>
}
