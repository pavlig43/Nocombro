package ru.pavlig43.immutable.internal.component.items.vendor

import ru.pavlig43.tablecore.model.ITableUi

data class VendorTableUi(
    val displayName: String,
    val comment: String,
    override val composeId: Int = 0,
): ITableUi