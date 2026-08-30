package ru.pavlig43.product.internal.update.tabs.label

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.thermallabel.api.component.ThermalLabelDialogComponent
import ru.pavlig43.thermallabel.api.data.ThermalLabelTemplateService

internal class ProductLabelComponent(
    componentContext: ComponentContext,
    private val productId: Int,
    private val getProductName: () -> String,
    private val thermalLabelTemplateService: ThermalLabelTemplateService,
) : ComponentContext by componentContext, FormTabComponent {

    override val title: String = "Этикетка"
    override val errorMessages: Flow<List<String>> = flowOf(emptyList())

    private val dialogNavigation = SlotNavigation<ProductLabelDialog>()

    val dialog: Value<ChildSlot<ProductLabelDialog, ThermalLabelDialogComponent>> = childSlot(
        source = dialogNavigation,
        key = "product_label_dialog",
        serializer = ProductLabelDialog.serializer(),
        handleBackButton = true,
        childFactory = { _, context ->
            ThermalLabelDialogComponent(
                componentContext = context,
                productId = productId,
                productName = getProductName(),
                defaultDate = getCurrentLocalDate(),
                service = thermalLabelTemplateService,
                onDismissed = dialogNavigation::dismiss,
            )
        },
    )

    fun openLabelDialog() {
        dialogNavigation.activate(ProductLabelDialog.Label)
    }

    override suspend fun onUpdate(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshDataAfterUpsert() = Unit
}

@Serializable
internal sealed interface ProductLabelDialog {
    @Serializable
    data object Label : ProductLabelDialog
}
