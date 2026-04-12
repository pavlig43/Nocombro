package ru.pavlig43.doctor.internal.ui.tool.filecleanup

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
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.doctor.internal.component.DoctorOrphanFilesLoadState
import ru.pavlig43.doctor.internal.component.DoctorRemoteOrphanFilesLoadState
import ru.pavlig43.doctor.internal.ui.common.DoctorSectionCard
import ru.pavlig43.doctor.internal.ui.common.toReadableFileSize
import ru.pavlig43.files.api.model.LocalOrphanFile
import ru.pavlig43.files.api.model.RemoteOrphanFile

@Composable
internal fun DoctorFileCleanupTool(
    state: DoctorOrphanFilesLoadState,
    actionError: String?,
    onDismissActionError: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    DoctorSectionCard(
        title = when (state) {
            is DoctorOrphanFilesLoadState.Success -> "Чистка файлов: ${state.files.size}"
            DoctorOrphanFilesLoadState.Loading -> "Чистка файлов"
            is DoctorOrphanFilesLoadState.Error -> "Чистка файлов"
        },
        subtitle = "Поиск локальных файлов, которых больше нет в таблице file.",
        headerActions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) {
                    Text("Обновить")
                }
                if (state is DoctorOrphanFilesLoadState.Success && state.files.isNotEmpty()) {
                    Button(onClick = onDeleteAll) {
                        Text("Удалить все")
                    }
                }
            }
        }
    ) {
        when (state) {
            DoctorOrphanFilesLoadState.Loading -> LoadingUi()
            is DoctorOrphanFilesLoadState.Error -> ValidationErrorsCard(
                errorMessages = listOf(state.message)
            )
            is DoctorOrphanFilesLoadState.Success -> {
                if (state.files.isEmpty()) {
                    Text(
                        text = "Локальных orphan-файлов нет.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.files.forEach { file ->
                            DoctorOrphanFileRow(
                                file = file,
                                onOpen = { onOpen(file.path) },
                                onDelete = { onDelete(file.path) },
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
private fun DoctorOrphanFileRow(
    file: LocalOrphanFile,
    onOpen: () -> Unit,
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
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Размер: ${file.sizeBytes.toReadableFileSize()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = file.relativePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) {
                    Text("Открыть")
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                ) {
                    Text("Удалить локально")
                }
            }
        }
    }
}

@Composable
internal fun DoctorRemoteFileCleanupTool(
    state: DoctorRemoteOrphanFilesLoadState,
    actionError: String?,
    isActionsEnabled: Boolean,
    statusMessage: String,
    onDismissActionError: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    DoctorSectionCard(
        title = when (state) {
            is DoctorRemoteOrphanFilesLoadState.Success -> "Чистка S3: ${state.files.size}"
            DoctorRemoteOrphanFilesLoadState.Loading -> "Чистка S3"
            is DoctorRemoteOrphanFilesLoadState.Error -> "Чистка S3"
        },
        subtitle = "Поиск remote-объектов в bucket, которых больше нет в таблице file. Запускать после sync/pull.",
        headerActions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = isActionsEnabled,
                ) {
                    Text("Обновить")
                }
                if (state is DoctorRemoteOrphanFilesLoadState.Success && state.files.isNotEmpty()) {
                    Button(
                        onClick = onDeleteAll,
                        enabled = isActionsEnabled,
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
            DoctorRemoteOrphanFilesLoadState.Loading -> LoadingUi()
            is DoctorRemoteOrphanFilesLoadState.Error -> ValidationErrorsCard(
                errorMessages = listOf(state.message)
            )
            is DoctorRemoteOrphanFilesLoadState.Success -> {
                if (state.files.isEmpty()) {
                    Text(
                        text = "Remote orphan-объектов нет.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.files.forEach { file ->
                            DoctorRemoteOrphanFileRow(
                                file = file,
                                enabled = isActionsEnabled,
                                onDelete = { onDelete(file.objectKey) },
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
private fun DoctorRemoteOrphanFileRow(
    file: RemoteOrphanFile,
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
                text = file.objectKey.substringAfterLast('/'),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            file.sizeBytes?.let { sizeBytes ->
                Text(
                    text = "Размер: ${sizeBytes.toReadableFileSize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = file.objectKey,
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
                    Text("Удалить из S3")
                }
            }
        }
    }
}
