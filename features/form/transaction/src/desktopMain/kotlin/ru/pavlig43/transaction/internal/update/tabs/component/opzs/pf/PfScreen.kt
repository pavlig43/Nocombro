package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineBlockScreen

@Composable
internal fun PfScreen(
    component: PfComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    SingleLineBlockScreen(component)

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is PfDialogChild.ImmutableMBS -> MBSImmutableTable(dialogChild.component)
        }
    }
}
