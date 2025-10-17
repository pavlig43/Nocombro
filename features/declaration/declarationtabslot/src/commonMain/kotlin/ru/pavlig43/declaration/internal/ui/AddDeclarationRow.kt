package ru.pavlig43.declaration.internal.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.coreui.tooltip.ProjectToolTip
import ru.pavlig43.declaration.api.data.ProductDeclarationUi

@Composable
internal fun AddDeclarationRow(
    productDeclarationUi: ProductDeclarationUi,
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
            text = "${productDeclarationUi.declarationName}\n от ${productDeclarationUi.vendorName}",
            textDecoration = TextDecoration.Underline,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(250.dp),
        )
        ProjectToolTip(
            tooltipText = if (productDeclarationUi.isActual) "Aктуальна" else "Срок истек",
            content = {
                Icon(
                    if (productDeclarationUi.isActual) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (productDeclarationUi.isActual) Color.Green else Color.Red
                )
            }

        )

        IconButtonToolTip(
            tooltipText = "Открыть в новой вкладке",
            onClick =  { openDeclarationDocument(productDeclarationUi.declarationId) },
            icon = Icons.Default.Search
        )

        IconButtonToolTip(
            tooltipText = "Удалить",
            onClick = { removeDeclarationUi(productDeclarationUi.composeKey) },
            icon = Icons.Default.Close
        )


    }

}