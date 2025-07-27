package ru.pavlig43.rootnocombro.internal.navigation.drawer.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.core.entries
import kotlin.reflect.KClass

internal class DrawerComponent(
    componentContext: ComponentContext,
    private val openScreen: DrawerDestination.() -> Unit,
) : ComponentContext by componentContext, IDrawerComponent {

    override fun onSelect(configuration: DrawerDestination) {
        openScreen(configuration)
    }

    private val _drawerConfigurationsState = MutableStateFlow(DrawerDestination::class.entries())

    override val drawerConfigurationsState: StateFlow<List<DrawerDestination>> =
        _drawerConfigurationsState.asStateFlow()


}
