package ru.pavlig43.vendor.internal

import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.VendorType
import ru.pavlig43.manageitem.api.data.RequireValues
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun RequireValues.toVendor(): Vendor {
    return Vendor(
        displayName = name,
        type = type as VendorType,
        createdAt = createdAt?.value?: Clock.System.now().toEpochMilliseconds(),
        comment = comment,
        id = id
    )
}