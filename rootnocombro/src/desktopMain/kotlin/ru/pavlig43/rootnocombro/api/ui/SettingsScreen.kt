package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.ProjectDialog
import ru.pavlig43.rootnocombro.api.component.SettingsComponent
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.dark_mode
import ru.pavlig43.theme.light_mode
import ru.pavlig43.theme.settings

@Composable
fun SettingsScreen(
    component: SettingsComponent,
) {
    val isOpened by component.isSettingsOpened.collectAsState()
    val darkMode by component.darkMode.collectAsState()

    if (isOpened) {
        ProjectDialog(
            onDismissRequest = component::closeSettings,
            header = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.settings),
                        contentDescription = null,
                    )
                    Text("Настройки")
                }
            },
            content = {
                Column(
                    modifier = Modifier
                        .width(560.dp)
                        .heightIn(max = 400.dp)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ThemeRow(
                        darkMode = darkMode,
                        onToggleDarkMode = component::toggleDarkMode,
                    )
                }
            }
        )
    }
}

@Composable
private fun ThemeRow(
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Тема",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        painter = painterResource(
                            if (darkMode) Res.drawable.dark_mode else Res.drawable.light_mode
                        ),
                        contentDescription = if (darkMode) "Тёмная тема" else "Светлая тема",
                    )
                }
            }
        }
    }
}
