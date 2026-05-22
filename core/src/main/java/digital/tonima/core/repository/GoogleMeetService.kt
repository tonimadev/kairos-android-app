package digital.tonima.core.repository

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GoogleMeetService {
    @POST("v2/spaces")
    suspend fun createSpace(
        @Header("Authorization") authorization: String,
        @Body request: CreateSpaceRequest = CreateSpaceRequest(),
    ): MeetSpace

    @GET("v2/{spaceName}/conferenceRecords")
    suspend fun getConferenceRecords(
        @Header("Authorization") authorization: String,
        @Path("spaceName", encoded = true) spaceName: String,
    ): ConferenceRecordsResponse

    @GET("v2/{conferenceRecordName}/transcripts")
    suspend fun getTranscripts(
        @Header("Authorization") authorization: String,
        @Path("conferenceRecordName", encoded = true) conferenceRecordName: String,
    ): TranscriptsResponse

    @GET("v2/{transcriptName}/entries")
    suspend fun getTranscriptEntries(
        @Header("Authorization") authorization: String,
        @Path("transcriptName", encoded = true) transcriptName: String,
    ): TranscriptEntriesResponse
}

@Serializable
class CreateSpaceRequest

@Serializable
data class MeetSpace(
    val name: String,
    val meetingUri: String,
    val meetingCode: String,
)

@Serializable
data class ConferenceRecordsResponse(
    val conferenceRecords: List<ConferenceRecord> = emptyList(),
)

@Serializable
data class ConferenceRecord(
    val name: String,
    val startTime: String? = null,
    val endTime: String? = null,
)

@Serializable
data class TranscriptsResponse(
    val transcripts: List<Transcript> = emptyList(),
)

@Serializable
data class Transcript(
    val name: String,
    val state: String? = null,
)

@Serializable
data class TranscriptEntriesResponse(
    val transcriptEntries: List<TranscriptEntry> = emptyList(),
)

@Serializable
data class TranscriptEntry(
    val name: String,
    val text: String,
    val participant: String? = null,
    val languageCode: String? = null,
)
