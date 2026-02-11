package ru.pavlig43.declaration.internal.model

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.emptyDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi
import kotlin.time.ExperimentalTime

data class DeclarationEssentialsUi(
    val displayName: String = "",
    val isObserveFromNotification: Boolean = true,
    val createdAt: LocalDate = getCurrentLocalDate(),
    val vendorId: Int? = null,
    val vendorName: String? = null,
    val bornDate: LocalDate = emptyDate,
    val bestBefore: LocalDate = emptyDate,
    val id: Int = 0,
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
        observeFromNotification = isObserveFromNotification
    )
}

internal fun Declaration.toUi(): DeclarationEssentialsUi {
    return DeclarationEssentialsUi(
        id = id,
        displayName = displayName,
        isObserveFromNotification = observeFromNotification,
        createdAt = createdAt,
        vendorId = vendorId,
        vendorName = vendorName,
        bestBefore = bestBefore
    )
}
