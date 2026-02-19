package ru.pavlig43.product.internal.update.tabs.declaration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.flowImmutable.api.ui.FlowMultiLineTableBox
import ru.pavlig43.immutable.api.ui.MBSImmutableTable

@Composable
internal fun DeclarationScreen(
    component: ProductDeclarationComponent
) {
    val dialog by component.dialog.subscribeAsState()
    FlowMultiLineTableBox(component)
    dialog.child?.instance?.also {
        MBSImmutableTable(it)
    }
}
