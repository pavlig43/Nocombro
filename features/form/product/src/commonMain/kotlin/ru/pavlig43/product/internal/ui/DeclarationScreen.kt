package ru.pavlig43.product.internal.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.coreui.tooltip.ToolTipProject
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.product.internal.component.tabs.component.DeclarationListComponent
import ru.pavlig43.product.internal.component.tabs.component.DeclarationUi
import ru.pavlig43.product.internal.component.tabs.component.ProductDeclarationComponent
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.add_circle
import ru.pavlig43.theme.check
import ru.pavlig43.theme.close
import ru.pavlig43.theme.delete
import ru.pavlig43.theme.search

@Composable
fun DeclarationScreen(
    component: ProductDeclarationComponent,
    modifier: Modifier = Modifier,
){
    val dialog by component.dialog.subscribeAsState()


    DeclarationBlock(
        component = component.declarationListComponent,
        onChooseDeclaration = component::openDialog,
        modifier = modifier.verticalScroll(rememberScrollState())
    )
    dialog.child?.instance?.also {
        MBSImmutableTable(it)
    }
}
@Composable
private fun DeclarationBlock(
    component: DeclarationListComponent,
    onChooseDeclaration: () -> Unit,

    modifier: Modifier = Modifier
) {
    val declarations by component.declarationList.collectAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Декларации")
            ToolTipIconButton(
                tooltipText = "Добавить декларацию",
                onClick = onChooseDeclaration,
                icon = Res.drawable.add_circle
            )
        }
        if (declarations.isEmpty()) {
            Text("Необходимо добавить декларацию", color = MaterialTheme.colorScheme.error)
        }
        declarations.forEach { declaration ->
            AddDeclarationRow1(
                itemDeclarationUi = declaration,
                openDeclarationDocument = { component.openDeclarationTab(it) },
                removeDeclarationUi = component::removeDeclaration
            )
        }

    }

}

@Composable
private fun AddDeclarationRow1(
    itemDeclarationUi: DeclarationUi,
    openDeclarationDocument: (Int) -> Unit,
    removeDeclarationUi: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary).padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = "${itemDeclarationUi.declarationName}\n от ${itemDeclarationUi.vendorName}",
            textDecoration = TextDecoration.Underline,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(250.dp),
        )
        ToolTipProject(
            tooltipText = if (itemDeclarationUi.isActual) "Aктуальна" else "Срок истек",
            content = {
                Icon(
                    painterResource(if (itemDeclarationUi.isActual) Res.drawable.check else Res.drawable.close),
                    contentDescription = null,
                    tint = if (itemDeclarationUi.isActual) Color.Green else Color.Red
                )
            }

        )

        ToolTipIconButton(
            tooltipText = "Открыть в новой вкладке",
            onClick =  { openDeclarationDocument(itemDeclarationUi.declarationId) },
            icon =Res.drawable.search
        )

        ToolTipIconButton(
            tooltipText = "Удалить",
            onClick = { removeDeclarationUi(itemDeclarationUi.composeKey) },
            icon = Res.drawable.delete
        )


    }

}