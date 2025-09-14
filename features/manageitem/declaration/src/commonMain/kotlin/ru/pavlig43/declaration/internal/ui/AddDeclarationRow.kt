package ru.pavlig43.declaration.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.declaration.api.data.DeclarationUi

@Composable
internal fun AddDeclarationRow(
    declarationUi: DeclarationUi,
    openDeclarationDocument: (Int) -> Unit,
    removeDeclarationUi: (Int) -> Unit,
    changeIsActual:(composeKey:Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButtonToolTip(
            tooltipText = "Сделать актуальной",
            onClick = { changeIsActual(declarationUi.composeKey) },
            icon = if (declarationUi.isActual) Icons.Default.Check else Icons.Default.DoNotDisturb,
            tint = if (declarationUi.isActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Text(
            text = declarationUi.name,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(250.dp),
        )

        IconButtonToolTip(
            tooltipText = "Открыть в новой вкладке",
            onClick =  { openDeclarationDocument(declarationUi.documentId) },
            icon = Icons.Default.Search
        )

        IconButtonToolTip(
            tooltipText = "Удалить",
            onClick = { removeDeclarationUi(declarationUi.composeKey) },
            icon = Icons.Default.Close
        )


    }

}