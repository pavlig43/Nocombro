package ru.pavlig43.rootnocombro.internal.di

import org.koin.dsl.module
import ru.pavlig43.datastore.SettingsRepository
import ru.pavlig43.rootnocombro.api.RootDependencies

internal fun settingsModule(rootDependencies: RootDependencies) = listOf(
    module {
        single<SettingsRepository> { rootDependencies.settingsRepository }
    }
)