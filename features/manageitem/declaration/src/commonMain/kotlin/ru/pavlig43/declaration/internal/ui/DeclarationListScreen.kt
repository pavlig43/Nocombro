package ru.pavlig43.declaration.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.data.DeclarationOut
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.declaration.api.component.DeclarationListComponent

@Composable
internal fun <D : DeclarationOut> DeclarationListScreen(
    component: DeclarationListComponent<D>,
    openChooseDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val declarations by component.declarationUi.collectAsState()
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Декларации")
            IconButtonToolTip(
                tooltipText = "Добавить декларацию",
                onClick = openChooseDialog,
                icon = Icons.Default.AddCircle
            )
        }
        if (declarations.isEmpty()) {
            Text("Необходимо добавить декларацию", color = MaterialTheme.colorScheme.error)
        }
        declarations.forEach { declaration ->
            AddDeclarationRow(
                declarationUi = declaration,
                openDeclarationDocument = {component.openDocumentTab(it)},
                changeIsActual = component::toggleIsActual,
                removeDeclarationUi = component::removeDeclaration
            )
        }

    }
}