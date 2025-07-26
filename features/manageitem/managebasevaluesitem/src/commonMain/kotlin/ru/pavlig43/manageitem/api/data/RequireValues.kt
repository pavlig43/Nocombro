package ru.pavlig43.manageitem.api.data

import ru.pavlig43.core.UTC
import ru.pavlig43.core.data.ItemType

data class RequireValues(
    val id: Int = 0,
    val name: String ="",
    val type: ItemType? = null,
    val createdAt:UTC? = null
)