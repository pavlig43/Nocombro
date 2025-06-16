package ru.pavlig43.product.api.component


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.database.data.product.Product

interface IProductComponent {
    val productState: StateFlow<ProductState>
}

interface ProductState {
    class Initial : ProductState
    class Loading : ProductState
    class Success(val data:Flow<List<Product>>) : ProductState
    class Error(val message: String) : ProductState
}