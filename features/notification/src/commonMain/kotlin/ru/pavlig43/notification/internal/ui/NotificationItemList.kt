package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.notification.internal.data.NotificationUi

@Suppress("MagicNumber")
@Composable
internal fun NotificationItemList(
    listNotificationUi:List<NotificationUi>,
    onOpenTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listNotificationUi.forEachIndexed { ind, notification ->
            val alpha = if (ind % 2 == 0) 1f else 0.5f
            NotificationRow(
                notification = notification,
                onOpenTab = onOpenTab,
                modifier = Modifier.background(MaterialTheme.colorScheme.onTertiary.copy(alpha = alpha))
            )
        }
    }

}

@Composable
private fun NotificationRow(
    notification: NotificationUi,
    onOpenTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().height(32.dp).clickable { onOpenTab(notification.id) }
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
            Text(notification.text)
        }
    }
}