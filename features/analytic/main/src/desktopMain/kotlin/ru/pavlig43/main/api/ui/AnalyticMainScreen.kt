package ru.pavlig43.main.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.main.api.component.AnalyticMainComponent
import ru.pavlig43.main.api.component.ItemNavigation

@Composable
fun AnalyticMainScreen(
    component: AnalyticMainComponent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Аналитика",
            style = MaterialTheme.typography.headlineMedium
        )
        ItemNavigation.entries.forEach {
            TextButton(
                onClick = { component.onOpenTab(it) }
            ) {
                Text(it.title)
            }
        }
    }
}

