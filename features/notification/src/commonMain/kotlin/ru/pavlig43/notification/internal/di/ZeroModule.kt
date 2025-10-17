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
    single<INotificationRepository>(zero(NotificationItem.Declaration)) {
        DeclarationZeroRepository(
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

private class DeclarationZeroRepository(
    db: NocombroDatabase
) : INotificationRepository {

    private val declarationWithoutDocument =
        db.declarationDao.observeOnDeclarationInWithoutFiles().map { lst ->
            lst.map { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = "В декларации ${notificationDTO.displayName} нет файлов"
                )
            }
        }
    private val getOnExpiredDeclaration =
        db.declarationDao.observeOnExpiredDeclaration(0).map { lst ->
            lst.map { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = "Декларации ${notificationDTO.displayName} просрочена"
                )
            }
        }


    override val notificationFlow: Flow<List<NotificationUi>> =
        combine(
            declarationWithoutDocument,
            getOnExpiredDeclaration
        ) { arrays ->
            arrays.flatMap { it }
        }

}

private class ProductNotificationRepository(
    db: NocombroDatabase
) : INotificationRepository {

    private val getProductWithExpiredDeclaration =
        db.productDeclarationDao.observeOnProductWithExpiredDeclaration().map { lst ->
            lst.map { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = "В продукте ${notificationDTO.displayName} просрочена декларация"
                )
            }
        }

    private val ingredientsNotEquals1000Gram =
        db.compositionDao.observeOnProductWhereIngredientsNotEquals1000gram().map { lst ->
            lst.map { notificationDTO ->
                val (product, composition) = notificationDTO.displayName.split(" @ ")
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


    override val notificationFlow: Flow<List<NotificationUi>> =
        combine(
            ingredientsNotEquals1000Gram,
            productWithoutComposition,
            getProductWithExpiredDeclaration
        ) { arrays ->
            arrays.flatMap { it }
        }

}
