package ru.pavlig43.core.tabs

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.value.Value


interface ITabNavigationComponent<TabConfiguration : Any, SlotComponent : Any> {
    val children: Value<Children<*, SlotComponent>>
    fun onSelectTab(index: Int)
    fun onMove(fromIndex: Int, toIndex: Int)
    fun onTabCloseClicked(index: Int)
    fun addTab(configuration: TabConfiguration)

    class Children<out C : Any, out T : Any>(
        val items: List<Child.Created<C, T>>,
        val selectedIndex: Int?,
    )
}