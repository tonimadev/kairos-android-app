package digital.tonima.core.usecases

import digital.tonima.core.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCurrentLocationUseCase
    @Inject
    constructor(
        private val locationRepository: LocationRepository,
    ) {
        suspend operator fun invoke() = locationRepository.getCurrentLocation()
    }
