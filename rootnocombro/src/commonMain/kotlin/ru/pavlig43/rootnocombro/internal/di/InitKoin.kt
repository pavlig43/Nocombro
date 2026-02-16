package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import ru.pavlig43.database.platformDataBaseModule
import ru.pavlig43.datastore.di.getSettingsRepository
import ru.pavlig43.rootnocombro.api.RootDependencies

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
//        logger(PrintLogger(Level.DEBUG))
        appDeclaration()
        modules(
            platformDataBaseModule() +
                    getSettingsRepository() +
                    appModule
        )

    }
}

private val appModule = module {
    singleOf(::RootDependencies)

}
