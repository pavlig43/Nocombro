package ru.pavlig43.rootnocombro.internal.navigation.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value

class NavigationComponent<NavigationConfiguration : Any, SlotComponent : Any>(
    componentContext: ComponentContext,
):ComponentContext by componentContext, INavigationComponent<NavigationConfiguration, SlotComponent>  {
    override val children: Value<INavigationComponent.Children<NavigationConfiguration, SlotComponent>>
        get() = TODO("Not yet implemented")

    override fun onSelect(index: Int) {
        TODO("Not yet implemented")
    }

    override fun addNavConfiguration(configuration: NavigationConfiguration) {
        TODO("Not yet implemented")
    }
}