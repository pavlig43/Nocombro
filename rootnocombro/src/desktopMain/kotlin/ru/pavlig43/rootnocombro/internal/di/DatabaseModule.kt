package ru.pavlig43.rootnocombro.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.S3RemoteFileStorageConfig
import ru.pavlig43.database.data.files.remote.S3RemoteFileStorageGateway
import ru.pavlig43.database.data.sync.SyncService
import ru.pavlig43.database.data.sync.SyncStateRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorHardDeleteRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPlanner
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationService
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRemoteGateway
import ru.pavlig43.database.data.sync.mirror.YdbJdbcMirrorSyncGateway
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
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
        single { SyncStateRepository(get<NocombroDatabase>().syncStateDao) }
        single { RemoteFileBatchDownloadRepository(get(), get()) }
        single { MirrorEntityApplyRepository(get()) }
        single { MirrorHardDeleteRepository(get()) }
        single { MirrorLocalSnapshotRepository(get()) }
        single { MirrorLocalApplyRepository(get(), get(), get()) }
        single { MirrorReconciliationPlanner() }
        single<MirrorSyncRemoteGateway> {
            val config = YdbMirrorJdbcConfig.fromEnvironment()
            YdbJdbcMirrorSyncGateway(config)
        }
        single { MirrorReconciliationService(get(), get(), get(), get()) }
        single { SyncService(get(), get(), get()) }
    },
)
