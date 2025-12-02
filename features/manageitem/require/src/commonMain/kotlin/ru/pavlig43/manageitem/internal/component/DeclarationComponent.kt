package ru.pavlig43.manageitem.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.*
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.itemlist.api.VendorListParamProvider
import ru.pavlig43.itemlist.api.component.MBSItemListComponent
import ru.pavlig43.manageitem.api.DeclarationFactoryParam
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.data.DeclarationEssentialsUi
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class DeclarationComponent(
    componentContext: ComponentContext,
    param: DeclarationFactoryParam,
    createEssentialsRepository: CreateEssentialsRepository<DeclarationIn>,
) : EssentialsComponent<DeclarationIn, DeclarationEssentialsUi>(
    componentContext = componentContext,
    initItem = DeclarationEssentialsUi(),
    isValidValuesFactory = {
        displayName.isNotBlank() && vendorId != null && bestBefore != null
    },
    onSuccessUpsert = param.onSuccessUpsert,
    vendorInfoForTabName = {declaration -> param.onChangeValueForMainTab(("*(Декларация) ${declaration.displayName}"))},
    upsertEssentialsRepository = createEssentialsRepository,
) {
    private val dialogNavigation = SlotNavigation<MBSVendorDialog>()

    internal val dialog: Value<ChildSlot<MBSVendorDialog, MBSItemListComponent>> =
        childSlot(
            source = dialogNavigation,
            key = "vendor_dialog",
            serializer = MBSVendorDialog.serializer(),
            handleBackButton = true,
        ) { config: MBSVendorDialog, context ->
            MBSItemListComponent(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                itemListDependencies = param.itemListDependencies,
                onCreate = { param.onOpenVendorTab(0) },
                itemListParamProvider = VendorListParamProvider(
                    withCheckbox = false
                ),
                onItemClick = {
                    changeVendor(it.id, it.displayName)
                    dialogNavigation.dismiss()
                },
            )
        }


    fun showDialog() {
        dialogNavigation.activate(MBSVendorDialog)
    }
    private fun changeVendor(vendorId: Int, vendorName: String) {
        val declaration = itemFields.value.copy(vendorId = vendorId, vendorName = vendorName)
        onChangeItem(declaration)

    }
}
@Serializable