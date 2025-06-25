package ru.pavlig43.rootnocombro.internal.navigation.component

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.value.Value

interface INavigationComponent<NavigationConfiguration : Any, SlotComponent : Any> {
    val children: Value<Children<NavigationConfiguration, SlotComponent>>
    fun addNavConfiguration(configuration: NavigationConfiguration)
    fun onSelect(index: Int)

    class Children<out C : Any, out T : Any>(
        val items: List<Child.Created<C, T>>,
        val selected: Int?,
    )
}