package ru.pavlig43.doctor.internal.ui.tool.storageoverview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.doctor.internal.component.DoctorStorageOverviewLoadState
import ru.pavlig43.doctor.internal.ui.common.DoctorSectionCard
import ru.pavlig43.doctor.internal.ui.common.toReadableFileSize

@Composable
internal fun DoctorStorageOverviewTool(
    state: DoctorStorageOverviewLoadState,
    onRefresh: () -> Unit,
) {
    DoctorSectionCard(
        title = "Обзор хранилища",
        subtitle = "Быстрая диагностика локального каталога файлов приложения.",
        headerActions = {
            OutlinedButton(onClick = onRefresh) {
                Text("Обновить")
            }
        }
    ) {
        when (state) {
            DoctorStorageOverviewLoadState.Loading -> LoadingUi()
            is DoctorStorageOverviewLoadState.Error -> ValidationErrorsCard(
                errorMessages = listOf(state.message)
            )
            is DoctorStorageOverviewLoadState.Success -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DoctorMetricCard(
                        title = "Корневой каталог",
                        value = state.overview.rootPath,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DoctorMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Локальные файлы",
                            value = state.overview.localFilesCount.toString(),
                        )
                        DoctorMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Суммарный размер",
                            value = state.overview.localFilesSizeBytes.toReadableFileSize(),
                        )
                        DoctorMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Orphan-файлы",
                            value = state.overview.orphanFilesCount.toString(),
                        )
                    }
                    Text(
                        text = "Этот экран нужен как быстрый health-check перед чисткой и дальнейшей диагностикой storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
