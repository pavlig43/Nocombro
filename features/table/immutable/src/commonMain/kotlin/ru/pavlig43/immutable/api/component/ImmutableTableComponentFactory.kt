package ru.pavlig43.immutable.api.component

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
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.component.items.declaration.DeclarationTableComponent
import ru.pavlig43.immutable.internal.component.items.document.DocumentTableComponent
import ru.pavlig43.immutable.internal.component.items.product.ProductTableComponent
import ru.pavlig43.immutable.internal.component.items.transaction.TransactionTableComponent
import ru.pavlig43.immutable.internal.component.items.vendor.VendorTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.immutable.internal.di.ImmutableTableRepositoryType
import ru.pavlig43.immutable.internal.di.moduleFactory
import ru.pavlig43.tablecore.model.ITableUi

class ImmutableTableComponentFactory(
    componentContext: ComponentContext,
    dependencies: ImmutableTableDependencies,
    private val immutableTableBuilderData: ImmutableTableBuilderData<out ITableUi>,
    private val onCreate: () -> Unit,
    private val onItemClick: (ITableUi) -> Unit,
) : ComponentContext by componentContext, SlotComponent {
    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(
        moduleFactory(
            dependencies
        )
    )
    private val _model = MutableStateFlow(SlotComponent.TabModel(immutableTableBuilderData.tabTitle))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    internal val tableComponent = build<ITableUi>(context = childContext("table"))

    @Suppress("UNCHECKED_CAST")
    private fun <I : ITableUi> build(
        context: ComponentContext,
    ): ImmutableTableComponent<*, I, *> {
        return when (immutableTableBuilderData) {
            is DocumentImmutableTableBuilder -> DocumentTableComponent(
                componentContext = context,
                tableBuilder = immutableTableBuilderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Document>>(
                    ImmutableTableRepositoryType.DOCUMENT.qualifier
                ),
            )

            is DeclarationImmutableTableBuilder -> DeclarationTableComponent(
                componentContext = context,
                tableBuilder = immutableTableBuilderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Declaration>>(
                    ImmutableTableRepositoryType.DECLARATION.qualifier
                ),
            )
            is ProductImmutableTableBuilder -> ProductTableComponent(
                componentContext = context,
                tableBuilder = immutableTableBuilderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Product>>(
                    ImmutableTableRepositoryType.PRODUCT.qualifier
                ),
            )
            is TransactionImmutableTableBuilder -> TransactionTableComponent(
                componentContext = context,
                tableBuilder = immutableTableBuilderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Transaction>>(
                    ImmutableTableRepositoryType.TRANSACTION.qualifier
                ),
            )
            is VendorImmutableTableBuilder -> VendorTableComponent(
                componentContext = context,
                tableBuilder = immutableTableBuilderData,
                onCreate = onCreate,
                onItemClick = onItemClick,
                repository = scope.get<ImmutableListRepository<Vendor>>(
                    ImmutableTableRepositoryType.VENDOR.qualifier
                ),
            )
        } as ImmutableTableComponent<*, I, *>
    }
}