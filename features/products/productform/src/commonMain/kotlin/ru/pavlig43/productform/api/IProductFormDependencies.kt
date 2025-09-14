package ru.pavlig43.productform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.api.data.ItemListRepository

interface IProductFormDependencies {
    val db: NocombroDatabase
    val transaction: DataBaseTransaction
    val documentListRepository: ItemListRepository<Document, DocumentType>
    val productListRepository: ItemListRepository<Product, ProductType>

}