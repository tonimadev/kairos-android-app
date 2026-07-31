package digital.tonima.core.repository

/**
 * Interface legacy que agrupa os novos repositórios para manter compatibilidade.
 * Recomendado injetar as interfaces
 * específicas ([AppMetadataRepository], [UserStatsRepository], [UserPreferencesRepository]) em novos componentes.
 */
interface AppStatusRepository :
    AppMetadataRepository, UserStatsRepository, UserPreferencesRepository
