package ru.pavlig43.vendor.internal.model

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

internal data class VendorEssentialsUi(
    val displayName: String = "",
    val comment: String = "",
    val id: Int = 0,
    val syncId: String = defaultSyncId(),
    val updatedAt: LocalDateTime = defaultUpdatedAt(),
    val deletedAt: LocalDateTime? = null,
): ISingleLineTableUi
internal fun Vendor.toUi(): VendorEssentialsUi {
    return VendorEssentialsUi(
        displayName = displayName,
        comment = comment,
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
internal fun VendorEssentialsUi.toDto(): Vendor {
    return Vendor(
        displayName = displayName,
        comment = comment,
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
