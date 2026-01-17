package ru.pavlig43.vendor.api

import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase

class VendorFormDependencies(
    val transaction: TransactionExecutor,
    val db: NocombroDatabase,
    val filesDependencies: FilesDependencies

    )