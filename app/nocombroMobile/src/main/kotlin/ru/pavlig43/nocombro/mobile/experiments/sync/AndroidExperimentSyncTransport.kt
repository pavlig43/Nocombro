package ru.pavlig43.nocombro.mobile.experiments.sync

import ru.pavlig43.nocombro.mobile.experiments.ExperimentSyncSnapshot
import ru.pavlig43.nocombro.mobile.experiments.ExperimentSyncTransport

/**
 * Данные доступа к Android sync backend.
 */
data class AndroidExperimentSyncCredentials(
    val endpoint: String,
    val database: String,
    val token: String,
)

/**
 * Источник sync credentials для Android transport.
 */
interface AndroidExperimentSyncCredentialsProvider {
    /**
     * Возвращает credentials или null, если sync не настроен.
     */
    suspend fun getCredentials(): AndroidExperimentSyncCredentials?
}

/**
 * Provider-заглушка до настройки Android sync credentials.
 */
class MissingAndroidExperimentSyncCredentialsProvider : AndroidExperimentSyncCredentialsProvider {
    override suspend fun getCredentials(): AndroidExperimentSyncCredentials? = null
}

/**
 * Android transport для синхронизации экспериментов.
 */
class AndroidExperimentSyncTransport(
    private val credentialsProvider: AndroidExperimentSyncCredentialsProvider,
) : ExperimentSyncTransport {
    override suspend fun sync(snapshot: ExperimentSyncSnapshot): Result<Unit> {
        val credentials = credentialsProvider.getCredentials()
            ?: return Result.failure(IllegalStateException("Доступ к Android YDB не настроен"))

        return Result.failure(
            UnsupportedOperationException(
                "Android YDB transport не реализован для ${credentials.endpoint}",
            )
        )
    }
}
