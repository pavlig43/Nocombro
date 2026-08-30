package ru.pavlig43.product.internal.update.tabs.label

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.thermallabel.api.ui.ThermalLabelDialog

@Composable
internal fun ProductLabelScreen(
    component: ProductLabelComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Button(onClick = component::openLabelDialog) {
            Text("Печать этикетки")
        }
    }

    dialog.child?.instance?.let { labelComponent ->
        ThermalLabelDialog(labelComponent)
    }
}
