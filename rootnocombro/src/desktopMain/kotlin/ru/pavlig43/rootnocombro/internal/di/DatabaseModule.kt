package ru.pavlig43.rootnocombro.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.S3RemoteFileStorageConfig
import ru.pavlig43.database.data.files.remote.S3RemoteFileStorageGateway
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.SyncEntityExportRepository
import ru.pavlig43.database.data.sync.SyncRemoteApplyRepository
import ru.pavlig43.database.data.sync.SyncRemoteGateway
import ru.pavlig43.database.data.sync.SyncRunner
import ru.pavlig43.database.data.sync.SyncService
import ru.pavlig43.database.data.sync.SyncStateRepository
import ru.pavlig43.database.data.sync.YdbJdbcConfig
import ru.pavlig43.database.data.sync.YdbJdbcSyncGateway
import ru.pavlig43.database.data.sync.YdbSyncGatewayMock
import ru.pavlig43.rootnocombro.api.RootDependencies


internal fun getDatabaseModule(rootDependencies: RootDependencies) = listOf(
    module {
        single<NocombroDatabase> { rootDependencies.database }
        single<TransactionExecutor> { NocombroTransactionExecutor(get()) }
        single<RemoteFileStorageGateway> {
            val config = S3RemoteFileStorageConfig.fromEnvironment()
            if (config != null) {
                S3RemoteFileStorageGateway(config)
            } else {
                NoopRemoteFileStorageGateway()
            }
        }
        single { SyncQueueRepository(get<NocombroDatabase>().syncDao) }
        single { SyncStateRepository(get<NocombroDatabase>().syncDao) }
        single { SyncRunner(get(), get()) }
        single { SyncEntityExportRepository(get()) }
        single { SyncRemoteApplyRepository(get()) }
        single<SyncRemoteGateway> {
            val config = YdbJdbcConfig.fromEnvironment()
            if (config != null) {
                YdbJdbcSyncGateway(config)
            } else {
                YdbSyncGatewayMock()
            }
        }
        single { SyncService(get(), get(), get(), get(), get(), get()) }
    }
)
