package ru.pavlig43.notification.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.data.NotificationUi

internal val zeroModule = module {
    single<INotificationRepository>(zero(NotificationItem.Document)) {
        DocumentNotificationRepository(
            get()
        )
    }
    single<INotificationRepository>(zero(NotificationItem.Product)) {
        ProductNotificationRepository(
            get()
        )
    }

}

private fun zero(unit: NotificationItem): Qualifier {
    return NotificationLevel.Zero.with(unit)
}

private class DocumentNotificationRepository(
    db: NocombroDatabase
) : INotificationRepository {
    override val notificationFlow: Flow<List<NotificationUi>> =
        db.documentDao.observeOnDocumentWithoutFiles().map { lst ->
            lst.map { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = "В документе ${notificationDTO.displayName} нет файлов"
                )
            }
        }
}

private class ProductNotificationRepository(
    db: NocombroDatabase
) : INotificationRepository {

    private val ingredientsNotEquals1000Gram =
        db.compositionDao.observeOnProductWhereIngredientsNotEquals1000gram().map { lst ->
            lst.map { notificationDTO ->
                val (product,composition) = notificationDTO.displayName.split(" @ ")
                NotificationUi(
                    id = notificationDTO.id,
                    text = "В продукте $product в составе $composition сумма ингредиентов не равна 1 кг "
                )
            }
        }
    private val productWithoutComposition =
        db.compositionDao.observeProductWithoutComposition().map { lst ->
            lst.map { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = "В продукте ${notificationDTO.displayName} нет состава"
                )
            }
        }
    private val productWithoutActualDeclaration =
        db.productDao.observeOnProductWithoutActualDeclaration().map { lst ->
            lst.map { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = "В продукте ${notificationDTO.displayName} нет актуальной декларации"
                )
            }
        }


    override val notificationFlow: Flow<List<NotificationUi>> =
        combine(
            ingredientsNotEquals1000Gram,
            productWithoutActualDeclaration,
            productWithoutComposition
        ) { arrays ->
            arrays.flatMap { it }
        }

}
