package ru.pavlig43.immutable.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.immutable.api.component.MutableTableComponentFactory

import ru.pavlig43.immutable.internal.ui.ImmutableTableBox
import ru.pavlig43.immutable.internal.ui.MutableTableBox

@Composable
fun MutableTableScreen(
    slotComponent: MutableTableComponentFactory
){
    MutableTableBox(slotComponent.tableComponent)
}