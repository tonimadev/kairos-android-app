package digital.tonima.core.data.usecases

import digital.tonima.core.permissions.PermissionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HasNotificationListenerAccessUseCase
    @Inject
    constructor(
        private val permissionManager: PermissionManager,
    ) {
        operator fun invoke(): Boolean = permissionManager.hasNotificationListenerAccess()
    }
