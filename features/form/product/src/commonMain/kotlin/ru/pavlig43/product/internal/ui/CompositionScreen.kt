package ru.pavlig43.product.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.ui.MutableTableBox
import ru.pavlig43.product.internal.component.tabs.component.composition.CompositionComponent


@Composable
internal fun CompositionScreen(
    component: CompositionComponent,
) {

    val dialog by component.dialog.subscribeAsState()

    MutableTableBox(component)

    dialog.child?.instance?.also {
        MBSImmutableTable(it)
    }



}


