package ru.pavlig43.notification.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.format
import kotlinx.datetime.plus
import kotlinx.datetime.until
import org.koin.dsl.module
import ru.pavlig43.core.mapValues
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.datetime.DateThreshold
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.model.NotificationUi

internal val highModule = module {
    registerRepository { DocumentZeroRepository(get()) }
    registerRepository { DeclarationZeroRepository(get()) }
    registerRepository { ProductZeroRepository(get()) }
    registerRepository { TransactionZeroRepository(get()) }
    registerRepository { BatchExpiryZeroRepository(get()) }
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
            arrays.toList().flatten()
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
            arrays.toList().flatten()
        }
}

private class TransactionZeroRepository(
    db: NocombroDatabase
) : INotificationRepository {

    override val notificationLevel: NotificationLevel = NotificationLevel.HIGH
    override val notificationItem: NotificationItem = NotificationItem.Transaction

    private val todayReminders =
        db.reminderDao.observeTodayReminders().mapValues { notificationDTO ->
            NotificationUi(
                id = notificationDTO.id,
                text = "Напоминание: ${notificationDTO.displayName}"
            )
        }

    override val mergedFromDBNotificationFlow: Flow<List<NotificationUi>> = todayReminders
}

private class BatchExpiryZeroRepository(
    db: NocombroDatabase
) : INotificationRepository {
    override val notificationLevel: NotificationLevel = NotificationLevel.HIGH
    override val notificationItem: NotificationItem = NotificationItem.BatchExpiry

    @Suppress("MagicNumber")
    override val mergedFromDBNotificationFlow: Flow<List<NotificationUi>> =
        db.batchMovementDao.observeAllMovementsWithBatch().map { allMovements ->
            val now = getCurrentLocalDate()
            allMovements
                .groupBy { it.movement.batchId }
                .values.mapNotNull { movements ->
                    val first = movements.first()
                    val product = first.batchOut.product
                    val batch = first.batchOut.batch
                    val shelfLifeDays = product.shelfLifeDays

                    if (shelfLifeDays <= 0) return@mapNotNull null

                    val balance = movements.fold(0L) { acc, out ->
                        when (out.movement.movementType) {
                            MovementType.INCOMING -> acc + out.movement.count
                            MovementType.OUTGOING -> acc - out.movement.count
                        }
                    }

                    if (balance <= 0) return@mapNotNull null

                    val expiryDate = batch.dateBorn.plus(shelfLifeDays, DateTimeUnit.DAY)
                    val daysRemaining = now.until(expiryDate, DateTimeUnit.DAY).toInt()
                    val threshold = (shelfLifeDays * 10) / 100

                    if (daysRemaining >= threshold) return@mapNotNull null

                    val transactionId = movements
                        .first { it.movement.movementType == MovementType.INCOMING }
                        .movement.transactionId

                    NotificationUi(
                        id = transactionId,
                        text = "Срок годности партии от ${batch.dateBorn.format(dateFormat)} " +
                                "продукта ${product.displayName} истекает. " +
                                "Осталось $daysRemaining дн."
                    )
                }
        }
}
