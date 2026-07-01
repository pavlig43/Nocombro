package ru.pavlig43.nocombro.mobile.experiments.internal.di

import org.koin.dsl.module
import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.internal.data.ExperimentDetailsRepository
import ru.pavlig43.nocombro.mobile.experiments.internal.data.ExperimentsListRepository

internal fun createMobileExperimentsModule(
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
                filesDirPath = dependencies.filesDirPath,
                fileProviderAuthority = dependencies.fileProviderAuthority,
            )
        }
    }
)
