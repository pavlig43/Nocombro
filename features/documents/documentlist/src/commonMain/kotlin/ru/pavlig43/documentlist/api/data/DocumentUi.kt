package ru.pavlig43.documentlist.api.data

import ru.pavlig43.database.data.common.data.ItemType
import ru.pavlig43.itemlist.api.data.ItemUi

data class DocumentUi(
    override val id: Int,
    override val displayName: String,
    override val type: ItemType,
    override val createdAt: String
) : ItemUi