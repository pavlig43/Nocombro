package ru.pavlig43.transaction.internal.update.tabs.component.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox

@Composable
internal fun SaleScreen(
    component: SaleComponent,
) {
    val dialog by component.dialog.subscribeAsState()
    MutableTableBox(component, tableSettingsModify = { ts -> ts.copy(showFooter = true) })

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.ImmutableMBS -> MBSImmutableTable(dialogChild.component)
        }
    }
}
