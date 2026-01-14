package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.rootnocombro.internal.navigation.IMainNavigationComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig
import ru.pavlig43.rootnocombro.internal.settings.component.ISettingsComponent
import ru.pavlig43.signroot.api.component.IRootSignComponent

interface IRootNocombroComponent {
    val stack: Value<ChildStack<*, Child>>

    val settingsComponent: ISettingsComponent

    sealed interface Child {
        class RootSign(val component: IRootSignComponent) : Child
        class Tabs(
            val component: IMainNavigationComponent<TabConfig, MainTabComponent>
        ):Child
    }

}