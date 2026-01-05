package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.StringColumnField

@Composable
fun NameFieldBlock(
    name: String,
    onChangeName:(String)-> Unit,
    modifier: Modifier = Modifier
){
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StringColumnField(
            value = name,
            onValueChange = onChangeName,
            headText = "Имя",

        )
        if (name.isBlank()) {
            Text("Имя не должно быть пустым")
        }
    }
}