package ru.pavlig43.transaction.internal.component.tabs.component.buy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DatePickerDialog
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.ui.MutableTableBox

@Composable
internal fun BuyScreen(
    component: BuyComponent,
) {
    val dialog by component.dialog.subscribeAsState()
    MutableTableBox(component)

    dialog.child?.instance?.also {dialogChild->
        when(dialogChild){
            is DialogChild.Date -> DatePickerDialog(dialogChild.component)
            is DialogChild.ImmutableMBS -> MBSImmutableTable(dialogChild.component)
        }

    }

}