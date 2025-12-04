package ru.pavlig43.productform.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.update.data.UpdateEssentialsRepository
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.productform.internal.data.ProductEssentialsUi
import ru.pavlig43.productform.internal.data.toDto

internal class EssentialTabSlot(
    componentContext: ComponentContext,
    productId: Int,
    updateRepository: UpdateEssentialsRepository<Product>,
    componentFactory: EssentialComponentFactory<Product, ProductEssentialsUi>,
) : UpdateEssentialsComponent<Product, ProductEssentialsUi>(
    componentContext = componentContext,
    id = productId,
    updateEssentialsRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = {toDto()}
), ProductTabSlot