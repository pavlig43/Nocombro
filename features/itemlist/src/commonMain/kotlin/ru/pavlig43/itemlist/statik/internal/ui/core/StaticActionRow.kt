package ru.pavlig43.itemlist.statik.internal.ui.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.ActionIconButton
import ru.pavlig43.itemlist.statik.internal.component.DeleteState

@Composable
internal fun StaticActionRow(
    delete: () -> Unit,
    deleteState: DeleteState,
    share: () -> Unit,
    modifier: Modifier = Modifier
) {
    println(deleteState)
    Row(
        modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (deleteState) {
            is DeleteState.Error -> {
                ActionIconButton(
                    icon = Icons.Sharp.Delete,
                    onClick = delete
                )
                Text(deleteState.message)
            }

            is DeleteState.Initial -> ActionIconButton(
                icon = Icons.Sharp.Delete,
                onClick = delete
            )

            is DeleteState.Loading -> {
                CircularProgressIndicator(Modifier.size(24.dp))
            }
            is DeleteState.Success -> {
                Text(deleteState.message)
            }
        }

        ActionIconButton(
            icon = Icons.Sharp.Share,
            onClick = share
        )
    }

}