@file:Suppress("TooManyFunctions")

package ru.pavlig43.nocombro.mobile.api.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.arkivanov.decompose.extensions.compose.stack.Children
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.nocombro.mobile.api.component.MobileChild
import ru.pavlig43.nocombro.mobile.api.component.NocombroMobileRootComponent
import ru.pavlig43.nocombro.mobile.experiments.api.ui.ExperimentsRoute
import ru.pavlig43.nocombro.mobile.sync.MobileSyncComponent
import ru.pavlig43.nocombro.mobile.sync.MobileSyncUiState
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.theme.NocombroTheme
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.description

/**
 * Root Compose UI Android-сборки.
 */
@Composable
fun NocombroMobileApp(
    component: NocombroMobileRootComponent,
) {
    NocombroTheme(
        darkTheme = true,
    ) {

        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            NocombroMobileContent(component)
        }
    }
}

/**
 * Выбирает Compose-экран по текущему Decompose child.
 */
@Composable
private fun NocombroMobileContent(
    component: NocombroMobileRootComponent,
) {
    Children(
        stack = component.stack,
        modifier = Modifier.fillMaxSize(),
    ) { child ->
        when (val instance = child.instance) {
            MobileChild.Menu -> MainMenuScreen(component)
            is MobileChild.Experiments -> ExperimentsRoute(
                component = instance.component,
                onOpenMenu = component::openMenu,
            )
        }
    }
}

/**
 * Главное меню Android-приложения с sync-card и списком разделов.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainMenuScreen(
    component: NocombroMobileRootComponent,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = { MainTopBar() },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MobileSyncMenuItem(
                    component = component.syncComponent,
                )
            }
            items(component.menuItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { component.selectMenuItem(item.config) },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Карточка sync-статуса и ручных действий в главном меню.
 */
@Composable
private fun MobileSyncMenuItem(
    component: MobileSyncComponent,
) {
    val state by component.uiState.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { component.toggleExpanded() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SyncHeader(state)
            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.expanded) {
                HorizontalDivider()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = component::check, enabled = !state.running) {
                        Text("Проверить")
                    }
                    OutlinedButton(onClick = component::push, enabled = !state.running) {
                        Text("Отправить")
                    }
                    OutlinedButton(onClick = component::pull, enabled = !state.running) {
                        Text("Получить")
                    }
                    Button(onClick = component::sync, enabled = !state.running) {
                        Text("Синхронизировать")
                    }
                    OutlinedButton(
                        onClick = component::openLatestReport,
                        enabled = !state.running && state.reportSnapshotAt != null,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.description),
                            contentDescription = "Изменения синхронизации",
                        )
                        Text("Изменения")
                    }
                }
                LastSyncText(state)
                ReportSnapshotText(state)
            }
        }
    }
}

/**
 * Текст последнего push/pull для sync-card.
 */
@Composable
private fun LastSyncText(state: MobileSyncUiState) {
    val pushAt = state.lastPushAt
    val pullAt = state.lastPullAt
    val text = when {
        (pushAt == null) && (pullAt == null) -> "Синхронизации ещё не было"
        (pushAt != null) && (pullAt != null) -> "Push: $pushAt  Pull: $pullAt"
        pushAt != null -> "Push: $pushAt"
        else -> "Pull: $pullAt"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
    )
}

/** Показывает время файла, который откроет кнопка «Изменения». */
@Composable
private fun ReportSnapshotText(state: MobileSyncUiState) {
    val text = state.reportSnapshotAt?.let { snapshotAt ->
        "Снимок от ${snapshotAt.format(dateTimeFormat)}. Не обновляется при открытии."
    } ?: "Отчёт появится после первой проверки."
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Заголовок sync-card со счётчиками local/remote changes.
 */
@Composable
private fun SyncHeader(state: MobileSyncUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Синхронизация",
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.running) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            text = "Local: ${state.localChanges}  Remote: ${state.remoteChanges}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Верхняя панель Android-приложения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        title = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Text(
                    text = "Nocombro",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    )
}
