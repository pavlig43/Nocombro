package ru.pavlig43.manageitem.api.data

import ru.pavlig43.core.data.ItemType


data class DefaultRequireValues(
    val id: Int = 0,
    val name: String = "",
    val type: ItemType? = null,
    val createdAt: Long? = null,
    val comment: String = ""
)
