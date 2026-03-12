package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.Module
import ru.pavlig43.rootnocombro.api.RootDependencies

internal fun createRootNocombroModule(
    rootDependencies: RootDependencies
): List<Module> {
    return featureDependenciesModule + getDatabaseModule(rootDependencies) + settingsModule(rootDependencies)

}