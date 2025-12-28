package ru.pavlig43.immutable.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.immutable.api.component.ImmutableTableComponentFactory

import ru.pavlig43.immutable.internal.ui.ImmutableTableBox

@Composable
fun ImmutableTableScreen(
    slotComponent: ImmutableTableComponentFactory
){
    ImmutableTableBox(slotComponent.tableComponent)
}