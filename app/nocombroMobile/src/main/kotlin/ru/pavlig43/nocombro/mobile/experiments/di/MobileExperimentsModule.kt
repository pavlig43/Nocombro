package ru.pavlig43.nocombro.mobile.experiments.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.pavlig43.nocombro.mobile.experiments.ExperimentSyncTransport
import ru.pavlig43.nocombro.mobile.experiments.ExperimentsMobileDependencies
import ru.pavlig43.nocombro.mobile.experiments.RoomExperimentsRepository
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase
import ru.pavlig43.nocombro.mobile.experiments.sync.AndroidExperimentSyncTransport
import ru.pavlig43.nocombro.mobile.experiments.sync.MissingAndroidExperimentSyncCredentialsProvider
import ru.pavlig43.nocombro.mobile.navigation.NocombroMobileRootDependencies

/**
 * App-level DI для mobile-фичи экспериментов и root mobile-компонента.
 */
val mobileExperimentsModule = module {
    single {
        MobileExperimentsDatabase.create(androidContext())
    }
    single<ExperimentSyncTransport> {
        AndroidExperimentSyncTransport(
            credentialsProvider = MissingAndroidExperimentSyncCredentialsProvider(),
        )
    }
    single {
        val database = get<MobileExperimentsDatabase>()
        val syncTransport = get<ExperimentSyncTransport>()
        ExperimentsMobileDependencies(
            repositoryFactory = { coroutineScope ->
                RoomExperimentsRepository(
                    db = database,
                    coroutineScope = coroutineScope,
                    syncTransport = syncTransport,
                )
            },
        )
    }
    single {
        NocombroMobileRootDependencies(
            experimentsDependencies = get(),
        )
    }
}
