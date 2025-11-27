package ru.pavlig43.declarationform.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.declarationform.internal.toDeclarationIn
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.itemlist.api.ItemListDependencies

internal class UpdateDeclarationTabSlot(
    componentContext: ComponentContext,
    itemListDependencies: ItemListDependencies,
    declarationId: Int,
    onOpenVendorTab: (Int) -> Unit,
    private val updateRepository: IUpdateRepository<DeclarationIn, DeclarationIn>,
    onChangeValueForMainTab: (String) -> Unit,
) : ComponentContext by componentContext, DeclarationTabSlot {

    override val title: String = "Основная информация"

    val requires = DeclarationRequiresComponent(
        componentContext = componentContext,
        onChangeValueForMainTab = onChangeValueForMainTab,
        getInitData = { updateRepository.getInit(declarationId) },
        openVendorTab = onOpenVendorTab,
        itemListDependencies = itemListDependencies
    )

    override suspend fun onUpdate() {
        val old = requires.initComponent.firstData.value?.toDeclarationIn()
        val new = requires.requiresValuesWithDate.value.toDeclarationIn()
        updateRepository.update(ChangeSet(old, new))
    }

}