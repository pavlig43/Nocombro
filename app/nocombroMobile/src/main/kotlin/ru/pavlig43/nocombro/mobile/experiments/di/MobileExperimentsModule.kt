package ru.pavlig43.nocombro.mobile.experiments.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.pavlig43.nocombro.mobile.experiments.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.ExperimentsListRepository
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase
import ru.pavlig43.nocombro.mobile.navigation.NocombroMobileRootDependencies

/**
 * App-level DI для mobile-фичи экспериментов и root mobile-компонента.
 */
val mobileExperimentsModule = module {
    single {
        MobileExperimentsDatabase.create(androidContext())
    }
    single {
        ExperimentsListRepository(
            db = get(),
        )
    }
    single {
        val database = get<MobileExperimentsDatabase>()
        ExperimentDependencies(
            database = database,
        )
    }
    single {
        NocombroMobileRootDependencies(
            experimentsDependencies = get(),
        )
    }
}
