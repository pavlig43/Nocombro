package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.platformDataBaseModule
import ru.pavlig43.datastore.getSettingsRepository
import ru.pavlig43.rootnocombro.api.RootDependencies

fun initKoin(
    databaseOverride: NocombroDatabase? = null,
    appDeclaration: KoinAppDeclaration = {},
) {
    startKoin {
//        logger(PrintLogger(Level.DEBUG))
        val databaseModule = databaseOverride?.let { database ->
            module {
                single<NocombroDatabase> { database }
            }
        } ?: platformDataBaseModule()
        modules(
            databaseModule +
                    getSettingsRepository() +
                    appModule
        )
        appDeclaration()

    }
}

private val appModule = module {
    singleOf(::RootDependencies)

}
