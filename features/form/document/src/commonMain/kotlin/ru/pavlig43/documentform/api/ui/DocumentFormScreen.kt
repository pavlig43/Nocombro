package ru.pavlig43.documentform.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.addfile.api.ui.FilesScreen
import ru.pavlig43.documentform.api.component.DocumentFormComponent
import ru.pavlig43.documentform.internal.component.tabs.tabslot.DocumentFileTabSlot
import ru.pavlig43.documentform.internal.component.tabs.tabslot.DocumentTabSlot
import ru.pavlig43.documentform.internal.component.tabs.tabslot.EssentialTabSlot
import ru.pavlig43.documentform.internal.ui.CreateDocumentScreen
import ru.pavlig43.documentform.internal.ui.DocumentFields
import ru.pavlig43.update.ui.ItemTabsUi
import ru.pavlig43.core.ui.EssentialBlockScreen

@Composable
fun DocumentFormScreen(
    component: DocumentFormComponent,
    modifier: Modifier = Modifier,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        val stack by component.stack.subscribeAsState()
        Children(
            stack = stack,
        ) { child ->
            when (val instance = child.instance) {
                is DocumentFormComponent.Child.Create -> CreateDocumentScreen(instance.component)
                is DocumentFormComponent.Child.Update -> ItemTabsUi(
                    component = instance.component,
                    slotFactory = { slotForm: DocumentTabSlot? ->
                        DocumentSlotScreen(slotForm)
                    })
            }
        }

    }

}

@Composable
private fun DocumentSlotScreen(documentSlot: DocumentTabSlot?) {
    when (documentSlot) {
        is DocumentFileTabSlot -> FilesScreen(documentSlot.fileComponent)
        is EssentialTabSlot -> EssentialBlockScreen(documentSlot) { item, onItemChange ->
            DocumentFields(
                item,
                onItemChange
            )
        }

        null -> Box(Modifier)
    }
}
