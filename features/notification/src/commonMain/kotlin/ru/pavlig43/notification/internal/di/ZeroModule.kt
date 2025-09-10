package ru.pavlig43.notification.internal.di

import kotlinx.coroutines.flow.Flow
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
    single<INotificationRepository> ( zero(NotificationItem.Product) ){ProductNotificationRepository(get())}

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
                    text = "В документе ${notificationDTO.name} нет файлов"
                )
            }
        }
}
private class ProductNotificationRepository(
    db: NocombroDatabase
) : INotificationRepository {

    override val notificationFlow: Flow<List<NotificationUi>> = db.productDao.observeOnProductWithoutActualDeclaration().map { lst->
        lst.map { notificationDTO ->
            NotificationUi(
                id = notificationDTO.id,
                text = "В продукте ${notificationDTO.name} нет актуальной декларации"
            )
        }
    }

}
