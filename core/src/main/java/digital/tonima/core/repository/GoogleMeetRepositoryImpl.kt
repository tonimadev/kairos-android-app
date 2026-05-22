package digital.tonima.core.repository

import com.paulrybitskyi.hiltbinder.BindType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.logcat
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = GoogleMeetRepository::class)
class GoogleMeetRepositoryImpl
    @Inject
    constructor(
        private val meetService: GoogleMeetService,
        private val authRepository: AuthRepository,
    ) : GoogleMeetRepository {
        override suspend fun createMeeting(): Result<MeetSpace> =
            withContext(Dispatchers.IO) {
                try {
                    val token = authRepository.getAccessToken()
                    if (token.isNullOrEmpty()) {
                        logcat { "Não foi possível criar reunião: Access Token não disponível." }
                        return@withContext Result.failure(Exception("Usuário não autenticado ou token inválido"))
                    }

                    val space =
                        meetService.createSpace(
                            authorization = "Bearer $token",
                        )

                    Result.success(space)
                } catch (e: Exception) {
                    logcat { "Erro ao criar espaço no Google Meet: ${e.message}" }
                    Result.failure(e)
                }
            }

        override suspend fun fetchMeetingTranscript(meetingCodeOrUri: String): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val token = authRepository.getAccessToken()
                    if (token.isNullOrEmpty()) {
                        return@withContext Result.failure(Exception("Usuário não autenticado ou token inválido"))
                    }

                    // Extrai o código "abc-defg-hij" da URI, se aplicável
                    val meetingCode =
                        if (meetingCodeOrUri.contains("meet.google.com/")) {
                            meetingCodeOrUri.substringAfter("meet.google.com/").substringBefore("?").trim()
                        } else {
                            meetingCodeOrUri.trim()
                        }

                    if (meetingCode.isBlank()) {
                        return@withContext Result.failure(Exception("Código de reunião inválido: $meetingCodeOrUri"))
                    }

                    val spaceName = "spaces/$meetingCode"
                    val authHeader = "Bearer $token"

                    // 1. Obter os Conference Records para esse espaço
                    val recordsResponse = meetService.getConferenceRecords(authHeader, spaceName)
                    if (recordsResponse.conferenceRecords.isEmpty()) {
                        return@withContext Result.failure(
                            Exception("Nenhum registro de conferência encontrado para a reunião $meetingCode"),
                        )
                    }

                    // Usamos o record mais recente
                    val latestRecord =
                        recordsResponse.conferenceRecords.maxByOrNull { it.startTime ?: "" }
                            ?: recordsResponse.conferenceRecords.first()

                    // 2. Obter as transcrições do record
                    val transcriptsResponse = meetService.getTranscripts(authHeader, latestRecord.name)
                    if (transcriptsResponse.transcripts.isEmpty()) {
                        return@withContext Result.failure(
                            Exception("Nenhuma transcrição disponível para esta reunião (pode não ter sido ativada)."),
                        )
                    }

                    val transcriptName = transcriptsResponse.transcripts.first().name

                    // 3. Obter as entradas da transcrição
                    val entriesResponse = meetService.getTranscriptEntries(authHeader, transcriptName)
                    if (entriesResponse.transcriptEntries.isEmpty()) {
                        return@withContext Result.failure(Exception("A transcrição está vazia."))
                    }

                    // 4. Montar o texto final
                    val sb = java.lang.StringBuilder()
                    entriesResponse.transcriptEntries.forEach { entry ->
                        val speaker = entry.participant?.substringAfterLast("/") ?: "Desconhecido"
                        sb.append("[$speaker]: ${entry.text}\n")
                    }

                    Result.success(sb.toString())
                } catch (e: Exception) {
                    logcat { "Erro ao buscar transcrição do Google Meet: ${e.message}" }
                    Result.failure(e)
                }
            }
    }
