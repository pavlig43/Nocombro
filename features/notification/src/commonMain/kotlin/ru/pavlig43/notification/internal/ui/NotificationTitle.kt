package ru.pavlig43.notification.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.api.ui.NotificationIcon

@Composable
internal fun NotificationTitle(
    isShowFullList: Boolean,
    showFullList: () -> Unit,
    title: String,
    level: NotificationLevel,
    countNotification: Int,
    modifier: Modifier = Modifier
) {

    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, textDecoration = TextDecoration.Underline)

            IconButtonToolTip(
                tooltipText = if (isShowFullList) "Свернуть" else " Показать",
                onClick = showFullList,
                icon = if (isShowFullList) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            )
            NotificationIcon(
                level = level,
                countNotification = countNotification
            )
        }

    }
}