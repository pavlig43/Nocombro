package ru.pavlig43.transaction.internal.update.tabs.component.sale

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox

@Composable
internal fun SaleScreen(
    component: SaleComponent,
) {
    val dialog by component.dialog.subscribeAsState()
    val enabledFillButton by component.enabledFillButton.collectAsState()
    val fillBatchesState by component.fillBatchesState.collectAsState()

    Column {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = component::fillByBatches,
                enabled = enabledFillButton,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Text("Заполнить по партиям")
            }
        }

        MutableTableBox(
            component = component,
            modifier = Modifier.weight(1f),
            tableSettingsModify = { settings -> settings.copy(showFooter = true) },
        )

        if (fillBatchesState is FillSaleBatchesState.Deficit) {
            ValidationErrorsCard(
                errorMessages = (fillBatchesState as FillSaleBatchesState.Deficit).messages,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.ImmutableMBS -> MBSImmutableTable(dialogChild.component)
        }
    }
}
