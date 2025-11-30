package ru.pavlig43.form.api.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.upsertitem.api.component.UpdateComponent

interface IItemFormInnerTabsComponent<Tab:Any,SlotComponent: FormTabSlot> {
    val mainTabTitle:StateFlow<String>
    val tabNavigationComponent: ITabNavigationComponent<Tab, SlotComponent>
    val updateComponent:UpdateComponent



}






