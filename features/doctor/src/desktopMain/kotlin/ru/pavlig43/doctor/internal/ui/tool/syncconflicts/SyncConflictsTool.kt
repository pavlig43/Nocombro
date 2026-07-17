package ru.pavlig43.doctor.internal.ui.tool.syncconflicts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.doctor.internal.component.DoctorSyncConflictView
import ru.pavlig43.doctor.internal.component.toDoctorView
import ru.pavlig43.doctor.internal.ui.common.DoctorSectionCard

/**
 * Показывает конфликты равных версий и просит подтвердить выбор стороны.
 *
 * Компонент сам не пишет данные. После подтверждения он передаёт исходный typed
 * конфликт в один из callback, а новая версия создаётся в sync-слое.
 */
@Composable
@Suppress("LongMethod")
internal fun DoctorSyncConflictsTool(
    conflicts: List<MirrorVersionConflict>,
    actionError: String?,
    onDismissError: () -> Unit,
    onUseLocal: (MirrorVersionConflict) -> Unit,
    onUseRemote: (MirrorVersionConflict) -> Unit,
) {
    val conflictViews = remember(conflicts) { conflicts.map { it.toDoctorView() } }
    var pendingResolution by remember { mutableStateOf<PendingConflictResolution?>(null) }

    DoctorSectionCard(
        title = "Конфликты sync: ${conflicts.size}",
        subtitle = "Строки различаются. Выбранная строка получит новую версию.",
    ) {
        if (conflicts.isEmpty()) {
            Text("Конфликтов нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                conflictViews.forEach { conflict ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Таблица: ${conflict.table}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text("sync_id: ${conflict.syncId}")
                        Text("Локальная версия: ${conflict.localVersion}")
                        Text("Удалённая версия: ${conflict.remoteVersion}")
                        Text("Локальная запись: ${conflict.localStatus.title}")
                        Text("Удалённая запись: ${conflict.remoteStatus.title}")
                        Text(
                            text = "Различающиеся поля:",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        conflict.differences.forEach { difference ->
                            Column(
                                modifier = Modifier.padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = difference.field,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text("Локально: ${difference.localValue}")
                                Text("Удалённо: ${difference.remoteValue}")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    pendingResolution = PendingConflictResolution(conflict, useLocal = true)
                                },
                            ) {
                                Text("Выбрать локальную")
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingResolution = PendingConflictResolution(conflict, useLocal = false)
                                },
                            ) {
                                Text("Выбрать удалённую")
                            }
                        }
                    }
                }
            }
        }
        actionError?.let { message ->
            ValidationErrorsCard(
                errorMessages = listOf(message),
                onErrorClick = { onDismissError() },
            )
        }
    }

    pendingResolution?.let { pending ->
        val sourceName = if (pending.useLocal) "локальную" else "удалённую"
        AlertDialog(
            onDismissRequest = { pendingResolution = null },
            title = { Text("Подтвердите выбор") },
            text = {
                Text(
                    "Выбрать $sourceName запись для " +
                        "${pending.conflict.table}/${pending.conflict.syncId}? " +
                        "Она получит новую версию и будет записана в Room и YDB."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pending.useLocal) {
                            onUseLocal(pending.conflict.source)
                        } else {
                            onUseRemote(pending.conflict.source)
                        }
                        pendingResolution = null
                    },
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingResolution = null }) {
                    Text("Отмена")
                }
            },
        )
    }
}

/** Выбор, ожидающий явного подтверждения в диалоге. */
private data class PendingConflictResolution(
    val conflict: DoctorSyncConflictView,
    val useLocal: Boolean,
)
