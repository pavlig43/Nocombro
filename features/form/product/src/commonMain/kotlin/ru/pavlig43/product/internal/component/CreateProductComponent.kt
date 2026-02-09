package ru.pavlig43.product.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.create.component.CreateEssentialsComponent
import ru.pavlig43.create.data.CreateSingleItemRepository
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.product.internal.data.ProductEssentialsUi
import ru.pavlig43.product.internal.data.toDto

internal class CreateProductComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createRepository: CreateSingleItemRepository<Product>,
    componentFactory: EssentialComponentFactory<Product, ProductEssentialsUi>
) : CreateEssentialsComponent<Product, ProductEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createSingleItemRepository = createRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },
)
