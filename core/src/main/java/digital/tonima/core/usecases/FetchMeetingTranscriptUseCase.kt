package digital.tonima.core.usecases

import digital.tonima.core.repository.GoogleMeetRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FetchMeetingTranscriptUseCase
    @Inject
    constructor(
        private val googleMeetRepository: GoogleMeetRepository,
    ) {
        suspend operator fun invoke(meetingUrl: String) = googleMeetRepository.fetchMeetingTranscript(meetingUrl)
    }
