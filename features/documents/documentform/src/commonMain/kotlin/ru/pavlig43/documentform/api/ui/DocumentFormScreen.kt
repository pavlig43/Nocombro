package ru.pavlig43.documentform.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.addfile.api.ui.FilesScreen
import ru.pavlig43.documentform.api.component.DocumentFormComponent
import ru.pavlig43.documentform.internal.component.tabs.DocumentFileTabSlot
import ru.pavlig43.documentform.internal.component.tabs.DocumentRequiresTabSlot
import ru.pavlig43.documentform.internal.component.tabs.DocumentTabSlot
import ru.pavlig43.form.api.ui.ItemTabsUi
import ru.pavlig43.manageitem.api.ui.CreateScreen
import ru.pavlig43.manageitem.api.ui.RequireValuesScreen

@Composable
fun DocumentFormScreen(
    component: DocumentFormComponent,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState)
    ) {
        val stack by component.stack.subscribeAsState()
        Children(
            stack = stack,
        ) { child ->
            when (val instance = child.instance) {
                is DocumentFormComponent.Child.Create -> CreateScreen(instance.component)
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
        is DocumentRequiresTabSlot -> RequireValuesScreen(documentSlot.requires)

        is DocumentFileTabSlot -> FilesScreen(documentSlot.fileComponent)
        else -> error("document slot not found $documentSlot")
    }
}
