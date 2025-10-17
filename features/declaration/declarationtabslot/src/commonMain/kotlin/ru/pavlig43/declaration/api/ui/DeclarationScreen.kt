package ru.pavlig43.declaration.api.ui

import DeclarationListScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.core.data.GenericDeclarationIn
import ru.pavlig43.core.data.GenericDeclarationOut
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.declaration.api.component.DeclarationTabSlot
import ru.pavlig43.declaration.api.component.ProductDeclarationListComponent
import ru.pavlig43.declaration.internal.component.MBSDeclarationListComponent
import ru.pavlig43.declaration.internal.ui.AddDeclarationRow

@Composable
fun<Out: GenericDeclarationOut,In: GenericDeclarationIn>ProductDeclarationScreen(
    component: DeclarationTabSlot<Out, In>,
    modifier: Modifier = Modifier,
){
    val dialog by component.dialog.subscribeAsState()

    DeclarationBlock(
        component = component.productDeclarationList,
        onChooseDeclaration = component::openDialog,
        modifier = modifier
    )
    dialog.child?.instance?.also {
        MBSDeclaration(it)
    }
}
@Composable
private fun <Out : GenericDeclarationOut> DeclarationBlock(
    component: ProductDeclarationListComponent<Out>,
    onChooseDeclaration: () -> Unit,

    modifier: Modifier = Modifier
) {
    val declarations by component.declarationUi.collectAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Декларации")
            IconButtonToolTip(
                tooltipText = "Добавить декларацию",
                onClick = onChooseDeclaration,
                icon = Icons.Default.AddCircle
            )
        }
        if (declarations.isEmpty()) {
            Text("Необходимо добавить декларацию", color = MaterialTheme.colorScheme.error)
        }
        declarations.forEach { declaration ->
            AddDeclarationRow(
                productDeclarationUi = declaration,
                openDeclarationDocument = {component.openDeclarationTab(it)},
                removeDeclarationUi = component::removeDeclaration
            )
        }

    }


}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun  MBSDeclaration(component: MBSDeclarationListComponent, modifier: Modifier = Modifier){
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = component::onDismissClicked,
        sheetState = sheetState,
        modifier = modifier.fillMaxSize()){
        DeclarationListScreen(component.declarationList,Modifier.fillMaxSize())
    }
}