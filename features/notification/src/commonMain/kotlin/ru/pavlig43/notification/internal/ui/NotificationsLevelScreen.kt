package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.notification.internal.component.INotificationItemComponent

@Composable
internal fun NotificationsLevelScreen(
    components: List<INotificationItemComponent>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.fillMaxWidth().padding(horizontal = 8.dp).border(1.dp, MaterialTheme.colorScheme.onBackground),
    ){
        items(components){
            NotificationItemBlock(it)
        }
    }
}





