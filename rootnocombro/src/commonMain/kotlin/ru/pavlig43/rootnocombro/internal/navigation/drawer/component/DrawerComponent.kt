package ru.pavlig43.rootnocombro.internal.navigation.drawer.component

import com.arkivanov.decompose.ComponentContext

class DrawerComponent<DrawerConfiguration : Any>(
    componentContext: ComponentContext,
    private val openScreen: (DrawerConfiguration) -> Unit,
    private val startConfiguration: List<DrawerConfiguration>
) : ComponentContext by componentContext, IDrawerComponent<DrawerConfiguration> {

    override fun onSelect(configuration: DrawerConfiguration) {
        openScreen(configuration)
    }

}