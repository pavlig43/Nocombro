package ru.pavlig43.nocombro.mobile.experiments.sync

import ru.pavlig43.nocombro.mobile.experiments.ExperimentSyncSnapshot
import ru.pavlig43.nocombro.mobile.experiments.ExperimentSyncTransport

data class AndroidExperimentSyncCredentials(
    val endpoint: String,
    val database: String,
    val token: String,
)

interface AndroidExperimentSyncCredentialsProvider {
    suspend fun getCredentials(): AndroidExperimentSyncCredentials?
}

class MissingAndroidExperimentSyncCredentialsProvider : AndroidExperimentSyncCredentialsProvider {
    override suspend fun getCredentials(): AndroidExperimentSyncCredentials? = null
}

class AndroidExperimentSyncTransport(
    private val credentialsProvider: AndroidExperimentSyncCredentialsProvider,
) : ExperimentSyncTransport {
    override suspend fun sync(snapshot: ExperimentSyncSnapshot): Result<Unit> {
        val credentials = credentialsProvider.getCredentials()
            ?: return Result.failure(IllegalStateException("Android YDB credentials are not configured"))

        return Result.failure(
            UnsupportedOperationException(
                "Android YDB transport is not implemented for endpoint ${credentials.endpoint}",
            )
        )
    }
}
