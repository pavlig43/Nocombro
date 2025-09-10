package ru.pavlig43.notification.internal.di

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.notification.api.INotificationDependencies
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.notification.api.data.NotificationLevel

private fun baseModule(dependencies: INotificationDependencies) = module {
    single<NocombroDatabase> { dependencies.db }
}
internal fun createNotificationModule(dependencies: INotificationDependencies) = listOf(
    baseModule(dependencies),
    zeroModule,
)
internal fun NotificationLevel.with(unit: NotificationItem): Qualifier {
    return named("${this.name}_${unit.name}")
}

internal fun one(unit: NotificationItem): Qualifier {
    return NotificationLevel.One.with(unit)
}
internal fun two(unit: NotificationItem): Qualifier {
    return NotificationLevel.Two.with(unit)
}