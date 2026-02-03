package ru.pavlig43.notification.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.dsl.module
import ru.pavlig43.core.DateThreshold
import ru.pavlig43.core.mapValues
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.model.NotificationUi

internal val mediumModule = module {

    registerRepository{ DeclarationOneRepository(get()) }



}

private class DeclarationOneRepository(
    db: NocombroDatabase
) : INotificationRepository {

    override val notificationLevel: NotificationLevel = NotificationLevel.MEDIUM
    override val notificationItem: NotificationItem = NotificationItem.Declaration
    private val getOnOneMountExpiredDeclaration: Flow<List<NotificationUi>> =
        db.declarationDao.observeOnExpiredDeclaration(DateThreshold.OneMonth).mapValues { notificationDTO ->
            NotificationUi(
                id = notificationDTO.id,
                text = notificationDTO.displayName
            )
        }

    override val mergedFromDBNotificationFlow: Flow<List<NotificationUi>> =
        combine(
            getOnOneMountExpiredDeclaration,
        ) { arrays ->
            arrays.flatMap { it }
        }
}
