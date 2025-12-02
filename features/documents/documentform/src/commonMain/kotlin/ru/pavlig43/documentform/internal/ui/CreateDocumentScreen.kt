package ru.pavlig43.documentform.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.documentform.internal.component.CreateDocumentComponent
import ru.pavlig43.manageitem.internal.ui.CreateEssentialsScreen

@Composable
internal fun CreateDocumentScreen(
    component: CreateDocumentComponent
) {
    CreateEssentialsScreen(component) { item, onItemChange ->
        DocumentFields(
            item,
            onItemChange
        )
    }
}
