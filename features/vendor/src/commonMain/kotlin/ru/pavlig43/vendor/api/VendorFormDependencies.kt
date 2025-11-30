package ru.pavlig43.vendor.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.manageitem.api.UpsertEssentialsDependencies

class VendorFormDependencies(
    val transaction: DataBaseTransaction,
    val db: NocombroDatabase,
    val upsertEssentialsDependencies: UpsertEssentialsDependencies,

    )