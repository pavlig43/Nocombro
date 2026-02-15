package ru.pavlig43.product.internal.update.tabs.declaration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.flowMiltiline.ui.FlowMultiLineTableBox

@Composable
internal fun DeclarationScreen(
    component: ProductDeclarationComponent1
) {
    val dialog by component.dialog.subscribeAsState()
    FlowMultiLineTableBox(component)
    dialog.child?.instance?.also {
        MBSImmutableTable(it)
    }
}
