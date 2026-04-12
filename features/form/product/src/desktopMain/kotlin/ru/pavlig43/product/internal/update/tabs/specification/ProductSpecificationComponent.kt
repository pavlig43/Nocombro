package ru.pavlig43.product.internal.update.tabs.specification

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec

internal class ProductSpecificationComponent(
    componentContext: ComponentContext,
    productId: Int,
    updateRepository: UpdateSingleLineRepository<ProductSpecification>,
) : UpdateSingleLineComponent<ProductSpecification, ProductSpecificationUi, ProductSpecificationField>(
    componentContext = componentContext,
    id = productId,
    updateSingleLineRepository = updateRepository,
    componentFactory = specificationComponentFactory,
    mapperToDTO = { toDto() }
) {
    override val title: String = "Спецификация"
    override val columns: ImmutableList<ColumnSpec<ProductSpecificationUi, ProductSpecificationField, Unit>> =
        createProductSpecificationColumns(
            onChangeItem = ::onChangeItem
        )

    override val errorMessages: Flow<List<String>> = errorTableMessages
}

private val specificationComponentFactory = SingleLineComponentFactory<ProductSpecification, ProductSpecificationUi>(
    initItem = ProductSpecificationUi(
        id = 0,
        productId = 0,
        description = "",
        dosage = "",
        composition = "",
        shelfLifeText = "",
        storageConditions = "",
        appearance = "",
        color = "",
        smell = "",
        taste = "",
        physicalChemicalIndicators = "",
        microbiologicalIndicators = "",
        toxicElements = "",
        allergens = "",
        gmoInfo = "",
        syncId = defaultSyncId(),
        updatedAt = defaultUpdatedAt(),
    ),
    errorFactory = { emptyList() },
    mapperToUi = { toUi() }
)
