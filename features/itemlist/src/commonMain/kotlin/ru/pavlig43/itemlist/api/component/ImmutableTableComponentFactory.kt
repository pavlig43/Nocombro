package ru.pavlig43.itemlist.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.qualifier.qualifier
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.itemlist.api.dependencies
import ru.pavlig43.itemlist.api.model.ITableUi
import ru.pavlig43.itemlist.internal.component.ImmutableTableComponent
import ru.pavlig43.itemlist.internal.component.items.declaration.DeclarationTableComponent
import ru.pavlig43.itemlist.internal.component.items.document.DocumentTableComponent
import ru.pavlig43.itemlist.internal.component.items.product.ProductTableComponent
import ru.pavlig43.itemlist.internal.component.items.transaction.TransactionTableComponent
import ru.pavlig43.itemlist.internal.component.items.vendor.VendorTableComponent
import ru.pavlig43.itemlist.internal.data.ImmutableListRepository
import ru.pavlig43.itemlist.internal.di.ImmutableTableRepositoryType
import ru.pavlig43.itemlist.internal.di.moduleFactory

class ImmutableTableComponentFactory(
    componentContext: ComponentContext,
    dependencies: dependencies,
    private val builderData: BuilderData<out ITableUi>,
    private val onCreate: () -> Unit,
    private val onItemClick: (ITableUi) -> Unit,
) : ComponentContext by componentContext, SlotComponent {
    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(
        moduleFactory(
            dependencies
        )
    )
    private val _model = MutableStateFlow(SlotComponent.TabModel(builderData.tabTitle))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    internal val tableComponent = build<ITableUi>(context = childContext("table"))

    @Suppress("UNCHECKED_CAST")
    private fun <I : ITableUi> build(
        context: ComponentContext,
    ): ImmutableTableComponent<*, I, *> {
        return when (builderData) {
            is DocumentBuilder -> DocumentTableComponent(
                componentContext = context,
                tableBuilder = builderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Document>>(
                    ImmutableTableRepositoryType.DOCUMENT.qualifier
                ),
            )

            is DeclarationBuilder -> DeclarationTableComponent(
                componentContext = context,
                tableBuilder = builderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Declaration>>(
                    ImmutableTableRepositoryType.DECLARATION.qualifier
                ),
            )
            is ProductBuilder -> ProductTableComponent(
                componentContext = context,
                tableBuilder = builderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Product>>(
                    ImmutableTableRepositoryType.PRODUCT.qualifier
                ),
            )
            is TransactionBuilder -> TransactionTableComponent(
                componentContext = context,
                tableBuilder = builderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Transaction>>(
                    ImmutableTableRepositoryType.TRANSACTION.qualifier
                ),
            )
            is VendorBuilder -> VendorTableComponent(
                componentContext = context,
                tableBuilder = builderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Vendor>>(
                    ImmutableTableRepositoryType.VENDOR.qualifier
                ),
            )
        } as ImmutableTableComponent<*, I, *>
    }
}