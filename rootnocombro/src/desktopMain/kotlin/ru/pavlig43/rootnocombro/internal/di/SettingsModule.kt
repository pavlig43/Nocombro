package ru.pavlig43.rootnocombro.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.datastore.SettingsRepository
import ru.pavlig43.datastore.SyncCheckAttemptStore
import ru.pavlig43.files.api.LocalFilesMaintenanceRepository
import ru.pavlig43.files.api.RemoteFilesMaintenanceRepository
import ru.pavlig43.rootnocombro.api.RootDependencies

internal fun settingsModule(rootDependencies: RootDependencies) = listOf(
    module {
        single<SettingsRepository> { rootDependencies.settingsRepository }
        single<SyncCheckAttemptStore> { rootDependencies.syncCheckAttemptStore }
        single { LocalFilesMaintenanceRepository(get<NocombroDatabase>()) }
        single { RemoteFilesMaintenanceRepository(get<NocombroDatabase>(), get(), get()) }
    }
)
