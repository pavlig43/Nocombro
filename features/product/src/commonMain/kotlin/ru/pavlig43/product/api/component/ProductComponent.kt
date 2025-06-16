package ru.pavlig43.product.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext

class ProductComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext, IProductComponent {
    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
//    private val scope: Scope =
//        koinContext.getOrCreateKoinScope()

    private val _productState = MutableStateFlow<ProductState>(ProductState.Initial())

    override val productState = _productState.asStateFlow()
}

