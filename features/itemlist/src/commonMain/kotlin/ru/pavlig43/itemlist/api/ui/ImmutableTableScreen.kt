package ru.pavlig43.itemlist.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.itemlist.api.component.ImmutableTableComponentFactory
import ru.pavlig43.itemlist.internal.ui.ImmutableTableBox

@Composable
fun ImmutableTableScreen(
    slotComponent: ImmutableTableComponentFactory
){
    ImmutableTableBox(slotComponent.tableComponent)
}