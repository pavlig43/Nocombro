package ru.pavlig43.declarationform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.VendorType
import ru.pavlig43.itemlist.api.data.IItemListRepository

interface IDeclarationDependencies {
    val transaction: DataBaseTransaction
    val db: NocombroDatabase
    val vendorRepository: IItemListRepository<Vendor, VendorType>

}