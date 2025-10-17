package ru.pavlig43.vendor.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase

interface IVendorFormDependencies {
    val transaction: DataBaseTransaction
    val db: NocombroDatabase
}