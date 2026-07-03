package ru.pavlig43.nocombro.mobile.internal.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.pavlig43.nocombro.mobile.api.component.NocombroMobileRootDependencies
import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase
import ru.pavlig43.nocombro.mobile.sync.MobileLocalMirrorRepository
import ru.pavlig43.nocombro.mobile.sync.MobileRemoteConfigRepository
import ru.pavlig43.nocombro.mobile.sync.MobileSyncRepository

/**
 * Koin-модуль Android-сборки: локальная БД, experiments и mobile sync.
 */
val nocombroMobileModule = module {
    single {
        NocombroMobileDatabase.create(androidContext())
    }
    single {
        val context = androidContext()
        ExperimentDependencies(
            database = get(),
            filesDirPath = context.filesDir.absolutePath,
            fileProviderAuthority = "${context.packageName}.fileprovider",
        )
    }
    single {
        MobileRemoteConfigRepository(androidContext())
    }
    single {
        MobileLocalMirrorRepository(
            db = get(),
            filesDirPath = androidContext().filesDir.absolutePath,
        )
    }
    single {
        MobileSyncRepository(
            configRepository = get(),
            localRepository = get(),
        )
    }
    single {
        NocombroMobileRootDependencies(
            experimentsDependencies = get(),
            syncRepository = get(),
        )
    }
}
