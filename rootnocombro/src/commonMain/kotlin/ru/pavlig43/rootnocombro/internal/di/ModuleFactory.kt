package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.Module
import ru.pavlig43.rootnocombro.api.IRootDependencies

internal fun createRootNocombroModule(
    rootDependencies: IRootDependencies
): List<Module> {
    return rootNocombroModule + getDatabaseModule(rootDependencies) + settingsModule(rootDependencies)

}