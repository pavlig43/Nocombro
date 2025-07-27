package ru.pavlig43.productlist.api

import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.product.dao.ProductDao

interface IProductLisDependencies {
    val productDao: ProductDao
}