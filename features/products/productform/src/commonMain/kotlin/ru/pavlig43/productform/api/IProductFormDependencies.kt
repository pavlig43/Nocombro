package ru.pavlig43.productform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.product.dao.ProductFilesDao
import ru.pavlig43.itemlist.api.data.ItemListRepository

interface IProductFormDependencies {
    val productDao: ProductDao
    val transaction: DataBaseTransaction
    val productFilesDao: ProductFilesDao
    val productDeclarationDao: ProductDeclarationDao
    val documentListRepository: ItemListRepository<Document, DocumentType>

}