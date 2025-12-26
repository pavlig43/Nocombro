package ru.pavlig43.itemlist.internal.component.items.vendor

import ru.pavlig43.itemlist.api.model.ITableUi

data class VendorTableUi(
    val displayName: String,
    val comment: String,
    override val composeId: Int = 0,
): ITableUi