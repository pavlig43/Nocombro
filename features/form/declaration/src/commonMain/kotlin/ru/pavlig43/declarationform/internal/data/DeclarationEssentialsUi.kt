package ru.pavlig43.declarationform.internal.data

import ru.pavlig43.core.data.ItemEssentialsUi
import ru.pavlig43.core.getUTCNow
import ru.pavlig43.database.data.declaration.Declaration
import kotlin.time.ExperimentalTime

data class DeclarationEssentialsUi(
    override val id: Int = 0,
    val displayName: String = "",
    val isObserveFromNotification: Boolean = true,
    val createdAt: Long? = null,
    val vendorId: Int? = null,
    val vendorName: String? = null,
    val bestBefore: Long? = null
) : ItemEssentialsUi

@Suppress("ThrowsCount")
@OptIn(ExperimentalTime::class)
internal fun DeclarationEssentialsUi.toDto(): Declaration {
    return Declaration(
        displayName = displayName,
        createdAt = createdAt ?: getUTCNow(),
        id = id,
        vendorId = vendorId ?: throw IllegalArgumentException("VendorId  required"),
        vendorName = vendorName ?: throw IllegalArgumentException("Vendor name required"),
        bestBefore = bestBefore ?: throw IllegalArgumentException("Best before required"),
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