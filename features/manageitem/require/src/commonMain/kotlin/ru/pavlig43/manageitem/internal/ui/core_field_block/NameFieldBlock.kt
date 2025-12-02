package ru.pavlig43.manageitem.internal.ui.core_field_block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.StringColumnField
import ru.pavlig43.manageitem.internal.ui.NAME

@Composable
fun NameFieldBlock(
    name: String,
    onChangeName:(String)-> Unit,
    modifier: Modifier = Modifier.Companion
){
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StringColumnField(
            value = name,
            onValueChange = onChangeName,
            headText = NAME,
        )
        if (name.isBlank()) {
            Text("Имя не должно быть пустым")
        }
    }
}