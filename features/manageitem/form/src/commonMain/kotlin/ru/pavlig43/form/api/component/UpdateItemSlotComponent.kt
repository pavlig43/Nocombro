package ru.pavlig43.form.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.manageitem.api.component.RequireValuesSlotComponent
import ru.pavlig43.manageitem.api.data.DefaultRequireValues

abstract class UpdateItemSlotComponent<I : Item, S : ItemType>(
    componentContext: ComponentContext,
    id: Int,
    typeVariantList: List<S>,
    private val mapper: DefaultRequireValues.()->I,
    private val updateRepository: IUpdateRepository<I,I>,
    onChangeValueForMainTab: (String) -> Unit,
) : ComponentContext by componentContext, FormTabSlot {

    override val title: String = "Основная информация"


    val requires = RequireValuesSlotComponent<I, S>(
        componentContext = childContext("require"),
        typeVariantList = typeVariantList,
        onChangeValueForMainTab = onChangeValueForMainTab,
        getInitData = { updateRepository.getInit(id) }
    )
    override suspend fun onUpdate() {
        val old = requires.initComponent.firstData.value?.mapper()
        val new = requires.requireValues.value.mapper()
        updateRepository.update(ChangeSet(old, new))
    }

}

