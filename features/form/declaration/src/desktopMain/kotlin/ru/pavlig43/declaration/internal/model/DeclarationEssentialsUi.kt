package ru.pavlig43.declaration.internal.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi
import kotlin.time.ExperimentalTime

data class DeclarationEssentialsUi(
    val displayName: String = "",
    val isObserveFromNotification: Boolean = true,
    val createdAt: LocalDate = getCurrentLocalDate(),
    val vendorId: Int? = null,
    val vendorName: String? = null,
    val bornDate: LocalDate = getCurrentLocalDate(),
    val bestBefore: LocalDate = getCurrentLocalDate(),
    val id: Int = 0,
    val syncId: String = defaultSyncId(),
    val updatedAt: LocalDateTime = defaultUpdatedAt(),
    val deletedAt: LocalDateTime? = null,
) : ISingleLineTableUi

@Suppress("ThrowsCount")
@OptIn(ExperimentalTime::class)
internal fun DeclarationEssentialsUi.toDto(): Declaration {
    return Declaration(
        displayName = displayName,
        createdAt = createdAt,
        id = id,
        vendorId = vendorId ?: throw IllegalArgumentException("VendorId  required"),
        vendorName = vendorName ?: throw IllegalArgumentException("Vendor name required"),
        bestBefore = bestBefore,
        bornDate = bornDate,
        observeFromNotification = isObserveFromNotification,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

internal fun Declaration.toUi(): DeclarationEssentialsUi {
    return DeclarationEssentialsUi(
        id = id,
        displayName = displayName,
        bornDate = bornDate,
        isObserveFromNotification = observeFromNotification,
        createdAt = createdAt,
        vendorId = vendorId,
        vendorName = vendorName,
        bestBefore = bestBefore,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
