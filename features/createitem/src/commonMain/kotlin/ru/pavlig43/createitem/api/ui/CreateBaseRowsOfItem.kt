package ru.pavlig43.createitem.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.createitem.api.component.ICreateItemComponent
import ru.pavlig43.createitem.api.component.ValidNameState
import ru.pavlig43.createitem.internal.ui.NAME

@Composable
fun CreateBaseRowsOfItem(
    component: ICreateItemComponent,
    modifier: Modifier = Modifier
) {
    val name by component.name.collectAsState()
    val type by component.type.collectAsState()
    val validNameState by component.isValidName.collectAsState()
    Column(modifier.fillMaxWidth()) {
        NameRow(
            name = name,
            onChangeName = component::onNameChange,
            validNameState = validNameState
        )
        Text(
            text = when (val state = validNameState) {
                is ValidNameState.Empty -> state.message
                is ValidNameState.Error -> state.message
                is ValidNameState.Initial -> ""
                is ValidNameState.Valid -> ""
                is ValidNameState.AllReadyExists -> state.message
            },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun NameRow(
    name: String,
    onChangeName: (String) -> Unit,
    validNameState: ValidNameState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = NAME)
        TextField(
            value = name,
            onValueChange = onChangeName,
            isError = validNameState !is ValidNameState.Valid
        )

    }
}