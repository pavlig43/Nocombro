package ru.pavlig43.vendor.internal

import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.manageitem.api.data.DefaultRequireValues
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun DefaultRequireValues.toVendor(): Vendor {
    return Vendor(
        displayName = name,
        comment = comment,
        id = id
    )
}