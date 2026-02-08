package ru.pavlig43.notification.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.dsl.module
import ru.pavlig43.core.DateThreshold
import ru.pavlig43.core.mapValues
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.model.NotificationUi

internal val lowModule = module {

    registerRepository { DeclarationTwoRepository(get()) }

}

private class DeclarationTwoRepository(
    db: NocombroDatabase
) : INotificationRepository {

    override val notificationLevel: NotificationLevel = NotificationLevel.LOW

    override val notificationItem: NotificationItem = NotificationItem.Declaration

    private val getOnThreeMountExpiredDeclaration =
        db.declarationDao.observeOnExpiredDeclaration(DateThreshold.ThreeMonth).mapValues { notificationDTO ->
            NotificationUi(
                id = notificationDTO.id,
                text = notificationDTO.displayName
            )
        }
    override val mergedFromDBNotificationFlow: Flow<List<NotificationUi>> =
        combine(
            getOnThreeMountExpiredDeclaration,
        ) { arrays ->
            arrays.toList().flatten()
        }
}