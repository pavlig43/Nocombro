package ru.pavlig43.nocombro.mobile.internal.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.pavlig43.nocombro.mobile.api.component.NocombroMobileRootDependencies
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase
import ru.pavlig43.nocombro.mobile.experiments.internal.data.ExperimentDetailsRepository
import ru.pavlig43.nocombro.mobile.experiments.internal.data.ExperimentsListRepository

val nocombroMobileModule = module {
    single {
        MobileExperimentsDatabase.create(androidContext())
    }
    single {
        ExperimentsListRepository(
            db = get(),
        )
    }
    single {
        ExperimentDependencies(database = get())
    }
    single {
        NocombroMobileRootDependencies(experimentsDependencies = get())
    }
}

internal fun createMobileExperimentsComponentModule(
    dependencies: ExperimentDependencies,
) = listOf(
    module {
        single {
            ExperimentsListRepository(
                db = dependencies.database,
            )
        }
        single {
            ExperimentDetailsRepository(
                db = dependencies.database,
            )
        }
    }
)
