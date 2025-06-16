package ru.pavlig43.document.api.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.createitem.api.ui.CreateBaseRowsOfItem
import ru.pavlig43.document.api.component.ICreateDocumentComponent

@Composable
fun CreateDocumentScreen(
    component: ICreateDocumentComponent,
    modifier: Modifier = Modifier,
) {
    Row {
        IconButton(component::onBack){
            Icon(Icons.Default.ArrowBack, contentDescription = null)
        }
    }
    CreateBaseRowsOfItem(component = component.createBaseRowsOfComponent)
}