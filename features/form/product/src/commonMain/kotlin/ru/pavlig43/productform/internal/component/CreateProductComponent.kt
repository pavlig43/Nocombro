package ru.pavlig43.productform.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.create.component.CreateEssentialsComponent
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.productform.internal.data.ProductEssentialsUi
import ru.pavlig43.productform.internal.data.toDto

internal class CreateProductComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createRepository: CreateEssentialsRepository<Product>,
    componentFactory: EssentialComponentFactory<Product, ProductEssentialsUi>
) : CreateEssentialsComponent<Product, ProductEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createEssentialsRepository = createRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },
)
