package ru.pavlig43.experiments.internal.di

import org.koin.dsl.module
import ru.pavlig43.experiments.api.ExperimentsDependencies
import ru.pavlig43.experiments.internal.data.ExperimentsRepository

internal fun createExperimentsModule(
    dependencies: ExperimentsDependencies,
) = listOf(
    module {
        single { ExperimentsRepository(dependencies.database, dependencies.syncQueueRepository) }
    }
)
