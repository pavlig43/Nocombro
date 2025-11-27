package ru.pavlig43.itemlist.api.data

import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.vendor.Vendor

class DocumentListRepository(
    db: NocombroDatabase
) {
    private val dao = db.documentDao
    private val tag = "DocumentListRepository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteDocumentsByIds(ids)
        }
    }

    fun observeOnItems(
        searchText: String,
        types: List<DocumentType>,
    ): Flow<RequestResult<List<Document>>> {
        return dbSafeFlow(tag) {
            dao.observeOnDocuments(
                searchText = searchText,
                types = types
            )
        }
    }
}
class DeclarationListRepository(
    db: NocombroDatabase
) {
    private val dao = db.declarationDao
    private val tag = "Declaration  list repository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteDeclarationsByIds(ids)
        }
    }


    fun observeDeclarationByFilter(
        text: String
    ): Flow<RequestResult<List<DeclarationIn>>> {

        return dbSafeFlow(tag) { dao.observeOnItems(text, text.isNotBlank()) }
    }
}
class ProductListRepository(
    db: NocombroDatabase
) {
    private val dao = db.productDao
    private val tag = "ProductListRepository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteProductsByIds(ids)
        }
    }

    fun observeOnItems(
        searchText: String,
        types: List<ProductType>,
    ): Flow<RequestResult<List<Product>>> {
        return dbSafeFlow(tag) {
            dao.observeOnProducts(
                searchText = searchText,
                types = types
            )
        }
    }
}
class VendorListRepository(
    db: NocombroDatabase
) {
    private val dao = db.vendorDao
    private val tag = "Vendor list repository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteVendorsByIds(ids)
        }
    }


    fun observeVendorByFilter(
        text: String
    ): Flow<RequestResult<List<Vendor>>> {

        return dbSafeFlow(tag) { dao.observeOnVendors(text) }
    }
}