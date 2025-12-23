package ru.pavlig43.rootnocombro.internal.di

import ru.pavlig43.rootnocombro.api.RootDependencies

internal fun createRootNocombroModule(
    rootDependencies: RootDependencies
): List<Module> {
    return featureDependenciesModule + getDatabaseModule(rootDependencies) + settingsModule(rootDependencies)

}