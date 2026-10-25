package ru.pavlig43.vendor.internal.model

import androidx.compose.foundation.text.input.TextFieldState
import ru.pavlig43.mutable.api.singleLine.component.SingleLineInputState
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

internal data class VendorEssentialsUi(
    val displayNameState: TextFieldState = TextFieldState(),
    val commentState: TextFieldState = TextFieldState(),
    val id: Int = 0,
    val syncId: String = defaultSyncId(),
    val updatedAt: LocalDateTime = defaultUpdatedAt(),
    val deletedAt: LocalDateTime? = null,
) : SingleLineInputState {
    val displayName: String get() = displayNameState.text.toString()
    val comment: String get() = commentState.text.toString()
    override val textFields get() = listOf(displayNameState, commentState)
}
internal fun Vendor.toUi(): VendorEssentialsUi {
    return VendorEssentialsUi(
        displayNameState = TextFieldState(displayName),
        commentState = TextFieldState(comment),
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
