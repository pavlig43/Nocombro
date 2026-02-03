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

internal val highModule = module {
    registerRepository { DocumentZeroRepository(get()) }
    registerRepository { DeclarationZeroRepository(get()) }
    registerRepository { ProductZeroRepository(get()) }

}

private class DocumentZeroRepository(
    db: NocombroDatabase
) : INotificationRepository {
    override val notificationLevel: NotificationLevel = NotificationLevel.HIGH
    override val notificationItem: NotificationItem = NotificationItem.Document
    override val mergedFromDBNotificationFlow: Flow<List<NotificationUi>> =
        db.documentDao.observeOnItemWithoutFiles().mapValues { notificationDTO ->
            NotificationUi(
                id = notificationDTO.id,
                text = "В документе ${notificationDTO.displayName} нет файлов"
            )
        }
}

private class DeclarationZeroRepository(
    db: NocombroDatabase
) : INotificationRepository {

    private val declarationWithoutDocument =
        db.declarationDao.observeOnItemWithoutFiles().mapValues { notificationDTO ->
            NotificationUi(
                id = notificationDTO.id,
                text = "В декларации ${notificationDTO.displayName} нет файлов"
            )
        }
    private val getOnExpiredDeclaration =
        db.declarationDao.observeOnExpiredDeclaration(DateThreshold.Now).mapValues { notificationDTO ->
            NotificationUi(
                id = notificationDTO.id,
                text = notificationDTO.displayName
            )
        }


    override val mergedFromDBNotificationFlow: Flow<List<NotificationUi>> =
        combine(
            declarationWithoutDocument,
            getOnExpiredDeclaration
        ) { arrays ->
            arrays.flatMap { it }
        }
    override val notificationLevel: NotificationLevel = NotificationLevel.HIGH
    override val notificationItem: NotificationItem = NotificationItem.Declaration

}

private class ProductZeroRepository(
    db: NocombroDatabase
) : INotificationRepository {

    override val notificationLevel: NotificationLevel = NotificationLevel.HIGH
    override val notificationItem: NotificationItem = NotificationItem.Product
    private val productDeclaration =
        db.productDeclarationDao.observeOnProductDeclarationNotification { db.productDao.observeOnProducts() }
            .mapValues { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = notificationDTO.displayName
                )
            }
    private val productComposition =
        db.compositionDao.observeProductWithoutComposition { db.productDao.observeOnProducts() }
            .mapValues { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = notificationDTO.displayName
                )
            }

    override val mergedFromDBNotificationFlow: Flow<List<NotificationUi>> =
        combine(
            productDeclaration,
            productComposition
        ) { arrays ->
            arrays.flatMap { it }
        }


}
