package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun IsObserveNotificationFieldBlock(
    isObserveFromNotification: Boolean,
    onCheckedNotificationVisible: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Отслеживать в оповещениях")
        Checkbox(
            checked = isObserveFromNotification,
            onCheckedChange = onCheckedNotificationVisible,
        )
    }
}