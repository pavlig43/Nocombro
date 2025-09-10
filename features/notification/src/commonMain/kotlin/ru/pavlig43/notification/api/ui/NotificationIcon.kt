package ru.pavlig43.notification.api.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pavlig43.notification.api.data.NotificationLevel

@Composable
fun NotificationIcon(
    level: NotificationLevel,
    countNotification: Int,
    modifier: Modifier = Modifier.size(36.dp)
) {
    Box(
        modifier = modifier
            .background(level.toColor(), MaterialTheme.shapes.medium)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countNotification.toString(),

            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.Black),

        )
    }
}
internal fun NotificationLevel.toColor(): Color {
    return when (this) {
        NotificationLevel.Zero -> Color.Red
        NotificationLevel.One -> Color.Yellow
        NotificationLevel.Two -> Color.Green
    }
}
