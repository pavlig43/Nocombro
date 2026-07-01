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
        val context = androidContext()
        ExperimentDependencies(
            database = get(),
            filesDirPath = context.filesDir.absolutePath,
            fileProviderAuthority = "${context.packageName}.fileprovider",
        )
    }
    single {
        NocombroMobileRootDependencies(experimentsDependencies = get())
    }
}
