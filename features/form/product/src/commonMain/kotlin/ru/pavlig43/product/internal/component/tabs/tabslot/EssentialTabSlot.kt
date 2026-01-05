package ru.pavlig43.product.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.product.internal.data.ProductEssentialsUi
import ru.pavlig43.product.internal.data.toDto
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateEssentialsRepository

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
), ProductTabSlot {
    override val errorMessages: Flow<List<String>> = itemFields.map { prod->
        buildList {
            if (prod.displayName.isBlank()){
                add("Имя продукта не может быть пустым")
            }

        }
    }
}