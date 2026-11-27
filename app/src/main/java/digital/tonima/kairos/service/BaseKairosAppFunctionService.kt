package digital.tonima.kairos.service

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.ai.appfunctions.KairosAppFunctions
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "KairosAppFunctionService",
    appFunctionXmlFileName = "kairos_app_function_service",
)
abstract class BaseKairosAppFunctionService : AppFunctionService() {
    @Inject
    internal lateinit var kairosAppFunctions: KairosAppFunctions

    /**
     * Cria um novo evento no calendário do usuário.
     * Use esta função quando o usuário pedir para agendar, criar ou adicionar um compromisso, reunião ou lembrete.
     *
     * @param title O título ou nome do evento (ex: "Reunião de Time").
     * @param startTime O horário de início em milissegundos desde a época (epoch millis).
     * @param endTime O horário de término em milissegundos desde a época (epoch millis).
     * Se não informado, presume-se que o evento dure 1 hora.
     * @param calendarId O identificador do calendário (use 1 como padrão se não souber).
     * Se esse calendário não existir, o evento é criado no primeiro calendário disponível.
     * @param description Notas ou detalhes adicionais sobre o compromisso.
     * @param location O local onde o evento ocorrerá (presencial ou link).
     * @param isAllDay Se verdadeiro, o evento será marcado para o dia inteiro, ignorando as horas.
     * @return O ID numérico do evento criado no banco de dados, ou -1 se falhar.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    internal suspend fun createEvent(
        title: String,
        startTime: Long,
        endTime: Long? = null,
        calendarId: Long = 1L,
        description: String? = null,
        location: String? = null,
        isAllDay: Boolean = false,
    ): Long =
        kairosAppFunctions.createEvent(
            title = title,
            startTime = startTime,
            endTime = endTime,
            calendarId = calendarId,
            description = description,
            location = location,
            isAllDay = isAllDay,
        )

    /**
     * Gera e retorna um resumo inteligente dos eventos e compromissos agendados para o dia de hoje.
     * O resumo inclui informações contextuais sobre o clima e proatividade.
     *
     * @return Um texto amigável com o briefing do dia.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    internal suspend fun getDailyBriefing(): String = kairosAppFunctions.getDailyBriefing()
}
