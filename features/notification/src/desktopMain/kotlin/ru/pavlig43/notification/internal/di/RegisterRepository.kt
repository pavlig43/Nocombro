package ru.pavlig43.notification.internal.di

import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.qualifier.TypeQualifier
import ru.pavlig43.notification.internal.data.INotificationRepository

internal inline fun <reified R : INotificationRepository> Module.registerRepository(
    noinline definition: Definition<R>
) {
    single<INotificationRepository>(
        qualifier = TypeQualifier(R::class),
        definition = definition
    )
}