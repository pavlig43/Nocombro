package ru.pavlig43.storage.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.core.SlotComponent

class StorageComponent(
    componentContext: ComponentContext
): ComponentContext by componentContext,SlotComponent{

    private val _model = MutableStateFlow(SlotComponent.TabModel("Склад"))
    override val model = _model.asStateFlow()


}