package ru.pavlig43.manageitem.api.data

import ru.pavlig43.core.UTC
import ru.pavlig43.database.data.common.data.ItemType

data class RequireValues(
    val id: Int,
    val name: String,
    val type: ItemType?,
    val createdAt:UTC?
)