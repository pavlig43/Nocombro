package ru.pavlig43.vendor.api

import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.immutable.api.ImmutableTableDependencies

class VendorFormDependencies(
    val transaction: DataBaseTransaction,
    val db: NocombroDatabase,
    val filesDependencies: FilesDependencies

    )