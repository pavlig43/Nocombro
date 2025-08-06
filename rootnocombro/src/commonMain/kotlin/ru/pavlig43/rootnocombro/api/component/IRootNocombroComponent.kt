package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.IMainNavigationComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.TabConfig
import ru.pavlig43.rootnocombro.internal.settings.component.ISettingsComponent
import ru.pavlig43.signroot.api.component.IRootSignComponent

interface IRootNocombroComponent {
    val stack: Value<ChildStack<*, Child>>

    val settingsComponent: ISettingsComponent

    sealed interface Child {
        class RootSign(val component: IRootSignComponent) : Child
        class Tabs(
            val component: IMainNavigationComponent<TabConfig, SlotComponent>
        ):Child
    }

}