package digital.tonima.core.repository

interface GoogleMeetRepository {
    suspend fun createMeeting(): Result<MeetSpace>

    /**
     * Tenta buscar a transcrição (se houver) de uma reunião passada a partir do código ou URI da reunião.
     * O meetingCode deve ser no formato "abc-defg-hij" ou a URI completa "https://meet.google.com/abc-defg-hij".
     */
    suspend fun fetchMeetingTranscript(meetingCodeOrUri: String): Result<String>
}
