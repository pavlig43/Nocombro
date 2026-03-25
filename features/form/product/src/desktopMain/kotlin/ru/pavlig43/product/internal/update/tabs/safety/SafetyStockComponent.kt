package ru.pavlig43.product.internal.update.tabs.safety

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec


internal class SafetyStockComponent(
    componentContext: ComponentContext,
    productId: Int,
    updateRepository: UpdateSingleLineRepository<SafetyStock>,
) : UpdateSingleLineComponent<SafetyStock, SafetyStockUi, SafetyStockField>(
    componentContext = componentContext,
    id = productId,
    updateSingleLineRepository = updateRepository,
    componentFactory = safetyStockComponentFactory,
    mapperToDTO = { toDto() }
) {
    override val title: String = "Нескончаемый"
    override val columns: ImmutableList<ColumnSpec<SafetyStockUi, SafetyStockField, Unit>> =
        createSafetyStockColumns(
            onChangeItem = ::onChangeItem
        )

    override val errorMessages: Flow<List<String>> = errorTableMessages
}

private val safetyStockComponentFactory = SingleLineComponentFactory<SafetyStock, SafetyStockUi>(
    initItem = SafetyStockUi(
        id = 0,
        productId = 0,
        reorderPoint = DecimalData3(0),
        orderQuantity = DecimalData3(0)
    ),
    errorFactory = { safety ->
        buildList {
            if (safety.reorderPoint.value == 0L && safety.orderQuantity.value != 0L)  add("Мало вероятно что такой нескончаемый остаток")
        }
    },
    mapperToUi = { toUi() }
)
