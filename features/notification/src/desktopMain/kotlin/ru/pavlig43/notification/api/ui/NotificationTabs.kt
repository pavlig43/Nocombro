package ru.pavlig43.notification.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.notification.api.component.NotificationComponent
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.internal.component.NotificationLevelComponent
import ru.pavlig43.notification.internal.ui.NotificationsLevelScreen

@Composable
fun NotificationTabs(
    pageNotificationComponent: NotificationComponent,
    modifier: Modifier = Modifier,
) {
    val children by pageNotificationComponent.tabNavigationComponent.tabChildren.subscribeAsState()
    val notificationCounts by pageNotificationComponent.notificationsForDrawer.collectAsState()
    val stateHolder = rememberSaveableStateHolder()
    val activeCount = notificationCounts.sumOf { it.count }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Оповещения",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "$activeCount активных",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            NotificationLevelPanel(
                levels = children.items.map { it.instance.component },
                selectedIndex = children.selectedIndex,
                onSelect = pageNotificationComponent.tabNavigationComponent::onSelectTab,
                modifier = Modifier
                    .fillMaxWidth(0.22f)
                    .widthIn(min = 220.dp, max = 280.dp)
                    .fillMaxHeight(),
            )

            val selectedIndex = children.selectedIndex
            if (selectedIndex != null) {
                val selectedComponent = children.items[selectedIndex].instance.component
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    stateHolder.SaveableStateProvider(selectedComponent.level.name) {
                        NotificationsLevelScreen(
                            component = selectedComponent,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationLevelPanel(
    levels: List<NotificationLevelComponent>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Уровень",
                style = MaterialTheme.typography.titleMedium,
            )
            levels.forEachIndexed { index, component ->
                val notificationBlocks by component.notificationFlow.collectAsState()
                NotificationLevelCard(
                    level = component.level,
                    count = notificationBlocks.sumOf { it.notificationList.size },
                    isSelected = selectedIndex == index,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun NotificationLevelCard(
    level: NotificationLevel,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(level.toColor(), CircleShape),
            )
            Text(
                text = level.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private val NotificationLevel.title: String
    get() = when (this) {
        NotificationLevel.HIGH -> "Срочные"
        NotificationLevel.MEDIUM -> "Важные"
        NotificationLevel.LOW -> "Прочие"
    }
