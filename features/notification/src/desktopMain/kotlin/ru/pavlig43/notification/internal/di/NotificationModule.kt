package ru.pavlig43.notification.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.notification.api.NotificationDependencies

internal fun createNotificationModule(dependencies: NotificationDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
    },
    highModule,
    mediumModule,
    lowModule
)

