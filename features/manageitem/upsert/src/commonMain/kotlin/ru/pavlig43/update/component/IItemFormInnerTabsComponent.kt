package ru.pavlig43.update.component

import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.tabs.TabNavigationComponent

interface IItemFormInnerTabsComponent<Tab:Any,SlotComponent: FormTabSlot> {
    val tabNavigationComponent: TabNavigationComponent<Tab, SlotComponent>
    val updateComponent: UpdateComponent



}