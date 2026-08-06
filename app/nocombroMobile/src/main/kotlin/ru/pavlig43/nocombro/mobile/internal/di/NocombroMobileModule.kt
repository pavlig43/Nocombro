package ru.pavlig43.nocombro.mobile.internal.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.pavlig43.nocombro.mobile.api.component.NocombroMobileRootDependencies
import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase
import ru.pavlig43.nocombro.mobile.sync.DataStoreMobileSyncStateStore
import ru.pavlig43.nocombro.mobile.sync.MobileLocalMirrorDataSource
import ru.pavlig43.nocombro.mobile.sync.MobileLocalMirrorRepository
import ru.pavlig43.nocombro.mobile.sync.MobileRemoteConfigSource
import ru.pavlig43.nocombro.mobile.sync.MobileRemoteConfigRepository
import ru.pavlig43.nocombro.mobile.sync.MobileSyncRepository
import ru.pavlig43.nocombro.mobile.sync.MobileSyncReportStore
import ru.pavlig43.nocombro.mobile.sync.MobileSyncStateStore
import ru.pavlig43.nocombro.mobile.warehouse.MobileWarehouseSnapshotStore

/**
 * Модуль Koin для Android-сборки: локальная БД, журнал экспериментов и мобильная синхронизация.
 */
val nocombroMobileModule = module {
    single {
        NocombroMobileDatabase.create(androidContext())
    }
    single<DataStore<Preferences>> {
        val context = androidContext()
        PreferenceDataStoreFactory.create {
            val file = context.filesDir.resolve("datastore/nocombro_mobile_sync.preferences_pb")
            file.parentFile?.mkdirs()
            file
        }
    }
    single<MobileSyncStateStore> {
        DataStoreMobileSyncStateStore(get())
    }

    single {
        val context = androidContext()
        ExperimentDependencies(
            database = get(),
            filesDirPath = context.filesDir.absolutePath,
            fileProviderAuthority = "${context.packageName}.fileprovider",
        )
    }
    single<MobileRemoteConfigSource> {
        MobileRemoteConfigRepository(androidContext())
    }
    single {
        MobileLocalMirrorRepository(
            db = get(),
            filesDirPath = androidContext().filesDir.absolutePath,
        )
    }
    single<MobileLocalMirrorDataSource> { get<MobileLocalMirrorRepository>() }
    single<MobileWarehouseSnapshotStore> { get<MobileLocalMirrorRepository>() }
    single {
        MobileSyncRepository(
            configRepository = get(),
            localRepository = get(),
        )
    }
    single {
        MobileSyncReportStore(androidContext().filesDir)
    }
    single {
        val context = androidContext()
        NocombroMobileRootDependencies(
            experimentsDependencies = get(),
            syncRepository = get(),
            syncStateStore = get(),
            syncReportStore = get(),
            warehouseSnapshotStore = get(),
            fileProviderAuthority = "${context.packageName}.fileprovider",
        )
    }
}
