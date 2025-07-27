package ru.pavlig43.productform.api.component

import ru.pavlig43.database.data.product.Product
import ru.pavlig43.manageitem.api.component.IManageBaseValueItemComponent
import ru.pavlig43.upsertitem.api.component.ISaveItemComponent

interface IProductFormComponent {
    val manageBaseValuesOfComponent: IManageBaseValueItemComponent
    val saveProductComponent:ISaveItemComponent<Product>
    fun closeScreen()

}

