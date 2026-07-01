package ru.pavlig43.nocombro.mobile.internal.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.pavlig43.nocombro.mobile.api.component.NocombroMobileRootDependencies
import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase

val nocombroMobileModule = module {
    single {
        NocombroMobileDatabase.create(androidContext())
    }
    single {
        ExperimentDependencies(database = get())
    }
    single {
        NocombroMobileRootDependencies(experimentsDependencies = get())
    }
}
