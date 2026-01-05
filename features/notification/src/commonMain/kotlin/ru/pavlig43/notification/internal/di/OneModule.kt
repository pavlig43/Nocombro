package ru.pavlig43.notification.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import ru.pavlig43.core.DateThreshold
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.data.NotificationUi

internal val oneModule = module {

    single<INotificationRepository>(one(NotificationItem.Declaration)) {
        DeclarationOneRepository(
            get()
        )
    }

}
private fun one(unit: NotificationItem): Qualifier {
    return NotificationLevel.One.with(unit)
}
private class DeclarationOneRepository(
    db: NocombroDatabase
) : INotificationRepository {

    private val getOnOneMountExpiredDeclaration =
        db.declarationDao.observeOnExpiredDeclaration(DateThreshold.OneMonth).map { lst ->
            lst.map { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = notificationDTO.displayName
                )
            }
        }
    override val notificationFlow: Flow<List<NotificationUi>> =
        combine(
            getOnOneMountExpiredDeclaration,
        ) { arrays ->
            arrays.flatMap { it }
        }
}