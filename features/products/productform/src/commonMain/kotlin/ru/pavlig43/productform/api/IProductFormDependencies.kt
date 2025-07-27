package ru.pavlig43.productform.api

import ru.pavlig43.database.data.product.dao.ProductDao

interface IProductFormDependencies {
    val productDao: ProductDao
}