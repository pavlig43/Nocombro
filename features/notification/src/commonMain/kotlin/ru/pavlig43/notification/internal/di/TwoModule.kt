package ru.pavlig43.notification.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.THREE_MONTH
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.data.NotificationUi

internal val twoModule = module {

    single<INotificationRepository>(two(NotificationItem.Declaration)) {
        DeclarationTwoRepository(
            get()
        )
    }

}
private fun two(unit: NotificationItem): Qualifier {
    return NotificationLevel.Two.with(unit)
}
private class DeclarationTwoRepository(
    db: NocombroDatabase
) : INotificationRepository {

    private val getOnThreeMountExpiredDeclaration =
        db.declarationDao.observeOnExpiredDeclaration(THREE_MONTH).map { lst ->
            lst.map { notificationDTO ->
                NotificationUi(
                    id = notificationDTO.id,
                    text = "Декларация ${notificationDTO.displayName} через 3 месяца или раньше просрочена"
                )
            }
        }
    override val notificationFlow: Flow<List<NotificationUi>> =
        combine(
            getOnThreeMountExpiredDeclaration,
        ) { arrays ->
            arrays.flatMap { it }
        }
}