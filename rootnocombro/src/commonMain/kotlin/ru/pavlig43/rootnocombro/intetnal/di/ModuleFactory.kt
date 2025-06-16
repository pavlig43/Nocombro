package ru.pavlig43.rootnocombro.intetnal.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.rootnocombro.api.IRootDependencies

internal fun createRootNocombroModule(
    rootDependencies: IRootDependencies
): List<Module> {
    return rootNocombroModule + getDatabaseModule(rootDependencies)

}