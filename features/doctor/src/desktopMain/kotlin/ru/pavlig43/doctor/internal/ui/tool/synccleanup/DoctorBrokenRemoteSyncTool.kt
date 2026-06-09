package ru.pavlig43.doctor.internal.ui.tool.synccleanup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.database.data.sync.BrokenRemoteSyncChange
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.doctor.internal.component.DoctorBrokenRemoteSyncLoadState
import ru.pavlig43.doctor.internal.ui.common.DoctorSectionCard

@Composable
internal fun DoctorBrokenRemoteSyncTool(
    state: DoctorBrokenRemoteSyncLoadState,
    actionError: String?,
    isActionsEnabled: Boolean,
    statusMessage: String,
    onDismissActionError: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: (BrokenRemoteSyncChange) -> Unit,
    onDeleteAll: () -> Unit,
) {
    DoctorSectionCard(
        title = when (state) {
            is DoctorBrokenRemoteSyncLoadState.Success -> "Чистка sync: ${state.changes.size}"
            DoctorBrokenRemoteSyncLoadState.Idle -> "Чистка sync"
            DoctorBrokenRemoteSyncLoadState.Loading -> "Чистка sync"
            is DoctorBrokenRemoteSyncLoadState.Error -> "Чистка sync"
        },
        subtitle = "Поиск и удаление битых remote-строк sync, которые не содержат payload для UPSERT.",
        headerActions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = isActionsEnabled,
                ) {
                    Text("Обновить")
                }
                if (state is DoctorBrokenRemoteSyncLoadState.Success && state.changes.isNotEmpty()) {
                    Button(
                        onClick = onDeleteAll,
                        enabled = isActionsEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    ) {
                        Text("Удалить все")
                    }
                }
            }
        }
    ) {
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        when (state) {
            DoctorBrokenRemoteSyncLoadState.Idle -> Text(
                text = "Нажмите \"Обновить\", чтобы загрузить битые remote-строки sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DoctorBrokenRemoteSyncLoadState.Loading -> LoadingUi()
            is DoctorBrokenRemoteSyncLoadState.Error -> ValidationErrorsCard(
                errorMessages = listOf(state.message)
            )
            is DoctorBrokenRemoteSyncLoadState.Success -> {
                if (state.changes.isEmpty()) {
                    Text(
                        text = "Битых remote-строк sync нет.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.changes.forEach { change ->
                            DoctorBrokenRemoteSyncRow(
                                change = change,
                                enabled = isActionsEnabled,
                                onDelete = { onDelete(change) },
                            )
                        }
                    }
                }
            }
        }

        actionError?.let { message ->
            Spacer(Modifier.height(8.dp))
            ValidationErrorsCard(
                errorMessages = listOf(message),
                onErrorClick = { onDismissActionError() },
            )
        }
    }
}

@Composable
private fun DoctorBrokenRemoteSyncRow(
    change: BrokenRemoteSyncChange,
    enabled: Boolean,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${change.entityTable}:${change.entitySyncId}",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Причина: ${change.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Тип: ${change.changeType.name} | Источник: ${change.sourceDeviceId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Изменено: ${change.changedAt.format(dateTimeFormat)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Курсор: ${change.cursor}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDelete,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                ) {
                    Text("Удалить строку")
                }
            }
        }
    }
}
