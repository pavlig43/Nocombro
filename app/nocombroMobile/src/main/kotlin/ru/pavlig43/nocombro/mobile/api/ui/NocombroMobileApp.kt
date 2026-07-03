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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.arkivanov.decompose.extensions.compose.stack.Children
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.nocombro.mobile.api.component.MobileChild
import ru.pavlig43.nocombro.mobile.api.component.NocombroMobileRootComponent
import ru.pavlig43.nocombro.mobile.experiments.api.ui.ExperimentsRoute
import ru.pavlig43.nocombro.mobile.sync.MobileSyncComponent
import ru.pavlig43.nocombro.mobile.sync.MobileEntityChange
import ru.pavlig43.nocombro.mobile.sync.MobileEntryChange
import ru.pavlig43.nocombro.mobile.sync.MobileExperimentChangeGroup
import ru.pavlig43.nocombro.mobile.sync.MobileFieldDiff
import ru.pavlig43.nocombro.mobile.sync.MobileSyncUiState
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
            is MobileChild.SyncChanges -> SyncChangesScreen(
                component = instance.component,
            )
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
                    onOpenChanges = component::openSyncChanges,
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
    onOpenChanges: () -> Unit,
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
                    IconButton(
                        onClick = onOpenChanges,
                        enabled = !state.running,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.description),
                            contentDescription = "Изменения синхронизации",
                        )
                    }
                }
                LastSyncText(state)
            }
        }
    }
}

/**
 * Экран preview: показывает, что уйдёт в push и что придёт в pull.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncChangesScreen(
    component: MobileSyncComponent,
) {
    val state by component.previewState.collectAsState()
    LaunchedEffect(component) {
        component.refreshPreview()
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Изменения") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                when {
                    state.loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    state.error != null -> Text(
                        text = state.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.localChanges.isEmpty() && state.remoteChanges.isEmpty() ->
                        Text("Изменений нет")
                }
            }
            changeSection(
                title = "К отправке",
                changes = state.localChanges,
            )
            changeSection(
                title = "К получению",
                changes = state.remoteChanges,
            )
        }
    }
}

/**
 * Добавляет секцию изменений в lazy list, если в ней есть элементы.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.changeSection(
    title: String,
    changes: List<MobileExperimentChangeGroup>,
) {
    if (changes.isEmpty()) return
    item {
        Text(
            text = "$title: ${changes.size}",
            style = MaterialTheme.typography.titleMedium,
        )
    }
    items(
        items = changes,
        key = MobileExperimentChangeGroup::experimentSyncId,
    ) { group ->
        ExperimentChangeCard(group)
    }
}

/**
 * Карточка изменений одного эксперимента.
 */
@Composable
private fun ExperimentChangeCard(
    group: MobileExperimentChangeGroup,
) {
    var expanded by rememberSaveable(group.experimentSyncId) { mutableStateOf(value = false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = group.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (expanded) "Свернуть" else "Открыть",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                group.metadata?.let { change ->
                    ChangeEntitySection(
                        title = "Метаданные",
                        changes = listOf(change),
                    )
                }
                ChangeEntitySection(
                    title = "Напоминания",
                    changes = group.reminders,
                )
                EntryChangeSection(group.entries)
            }
        }
    }
}

/**
 * Секция изменений однотипных сущностей.
 */
@Composable
private fun ChangeEntitySection(
    title: String,
    changes: List<MobileEntityChange>,
) {
    if (changes.isEmpty()) return
    HorizontalDivider()
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    changes.forEach { change ->
        ChangeEntityBlock(change)
    }
}

/**
 * Секция изменений записей эксперимента.
 */
@Composable
private fun EntryChangeSection(
    entries: List<MobileEntryChange>,
) {
    if (entries.isEmpty()) return
    HorizontalDivider()
    Text(
        text = "Записи",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    entries.forEach { entry ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${entry.title} · ${entry.actionLabel}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            entry.diffs.forEach { diff -> FieldDiffRow(diff) }
            if (entry.files.isNotEmpty()) {
                Text(
                    text = "Файлы",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                entry.files.forEach { file -> ChangeEntityBlock(file) }
            }
        }
    }
}

/**
 * Блок diff-а одной сущности.
 */
@Composable
private fun ChangeEntityBlock(
    change: MobileEntityChange,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "${change.title} · ${change.actionLabel}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        change.diffs.forEach { diff -> FieldDiffRow(diff) }
    }
}

/**
 * Строка diff-а одного поля.
 */
@Composable
private fun FieldDiffRow(
    diff: MobileFieldDiff,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = diff.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Было: ${diff.before}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Стало: ${diff.after}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
