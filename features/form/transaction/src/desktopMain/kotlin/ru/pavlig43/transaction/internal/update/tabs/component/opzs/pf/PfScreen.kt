package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineBlockScreen
import ru.pavlig43.thermallabel.api.ui.ThermalLabelDialog

@Composable
internal fun PfScreen(
    component: PfComponent,
) {
    val dialog by component.dialog.subscribeAsState()
    val isLabelButtonEnabled by component.isLabelButtonEnabled.collectAsState(initial = false)

    SingleLineBlockScreen(
        component = component,
        headerContent = {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = component::openThermalLabelDialog,
                    enabled = isLabelButtonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                ) {
                    Text("Этикетка")
                }
            }
        }
    )

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is PfDialogChild.ImmutableMBS -> MBSImmutableTable(dialogChild.component)
            is PfDialogChild.ThermalLabel -> ThermalLabelDialog(dialogChild.component)
        }
    }
}
