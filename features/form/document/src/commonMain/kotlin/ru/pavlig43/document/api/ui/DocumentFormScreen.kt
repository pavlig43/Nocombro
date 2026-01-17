package ru.pavlig43.document.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.files.api.ui.FilesScreen
import ru.pavlig43.core.ui.EssentialBlockScreen
import ru.pavlig43.document.api.component.DocumentFormComponent
import ru.pavlig43.document.internal.component.tabs.tabslot.DocumentEssentialComponent
import ru.pavlig43.document.internal.component.tabs.tabslot.DocumentTabChild
import ru.pavlig43.document.internal.ui.CreateDocumentScreen
import ru.pavlig43.document.internal.ui.DocumentFields
import ru.pavlig43.update.ui.FormTabsUi

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
                is DocumentFormComponent.Child.Update -> FormTabsUi(
                    component = instance.component,
                    tabChildFactory = { child: DocumentTabChild?->
                        DocumentFormTabScreen(child)
                    }
                )
            }
        }

    }

}

@Composable
private fun DocumentFormTabScreen(
    documentChild: DocumentTabChild?,
) {
    when (documentChild) {

        is DocumentTabChild.Essentials -> {
            UpdateEssentialsBlock(documentChild.component)}
        is DocumentTabChild.Files -> FilesScreen(documentChild.component)
        null -> Box(Modifier.fillMaxSize()){Text("Пусто")}
    }
}
@Composable
private fun UpdateEssentialsBlock(
    documentSlot: DocumentEssentialComponent,
    modifier: Modifier = Modifier
){
    Column(modifier.verticalScroll(rememberScrollState())){
        EssentialBlockScreen(documentSlot) { item, onItemChange ->
            DocumentFields(
                item,
                onItemChange
            )
        }
    }
}

