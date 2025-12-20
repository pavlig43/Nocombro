package ru.pavlig43.itemlist.internal.component.items.vendor

import ru.pavlig43.itemlist.api.model.IItemUi

data class VendorItemUi(
    val displayName: String,
    val comment: String,
    override val id: Int = 0,
): IItemUi