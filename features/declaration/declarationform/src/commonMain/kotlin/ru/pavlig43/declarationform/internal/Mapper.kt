package ru.pavlig43.declarationform.internal

import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.declarationform.internal.data.RequiresValuesWithDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


internal fun DeclarationIn.toDeclarationWithDate(): RequiresValuesWithDate {
    return RequiresValuesWithDate(
        name = displayName,
        createdAt = createdAt,
        id = id,
        vendorId = vendorId,
        bestBefore = bestBefore,
        vendorName = vendorName,
        isObserveFromNotification = observeFromNotification
    )
}
@OptIn(ExperimentalTime::class)
internal fun RequiresValuesWithDate.toDeclarationIn(): DeclarationIn{
    return DeclarationIn(
        displayName = name,
        createdAt = createdAt ?: Clock.System.now().toEpochMilliseconds(),
        vendorId = vendorId as Int,
        vendorName = vendorName as String,
        bestBefore = bestBefore as Long,
        id = id,
        observeFromNotification = isObserveFromNotification
    )
}