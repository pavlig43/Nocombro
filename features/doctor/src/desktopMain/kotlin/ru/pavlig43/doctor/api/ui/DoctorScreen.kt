package ru.pavlig43.doctor.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.doctor.api.component.DoctorComponent
import ru.pavlig43.doctor.internal.component.DoctorTool
import ru.pavlig43.doctor.internal.ui.common.DoctorSectionCard
import ru.pavlig43.doctor.internal.ui.common.DoctorToolCard
import ru.pavlig43.doctor.internal.ui.tool.filecleanup.DoctorFileCleanupTool
import ru.pavlig43.doctor.internal.ui.tool.storageoverview.DoctorStorageOverviewTool

@Composable
fun DoctorScreen(
    component: DoctorComponent,
) {
    val selectedTool by component.selectedTool.collectAsState()
    val storageOverviewState by component.storageOverviewState.collectAsState()
    val orphanFilesState by component.orphanFilesState.collectAsState()
    val orphanFilesActionError by component.orphanFilesActionError.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DoctorSectionCard(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight(),
            title = "Инструменты",
            subtitle = "Разделы диагностики и обслуживания.",
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 2.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(DoctorTool.entries, key = { it.name }) { tool ->
                    DoctorToolCard(
                        tool = tool,
                        isSelected = tool == selectedTool,
                        onClick = { component.selectTool(tool) },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (selectedTool) {
                    DoctorTool.StorageOverview -> DoctorStorageOverviewTool(
                        state = storageOverviewState,
                        onRefresh = component::refreshStorageOverview,
                    )

                    DoctorTool.FileCleanup -> DoctorFileCleanupTool(
                        state = orphanFilesState,
                        actionError = orphanFilesActionError,
                        onDismissActionError = component::dismissOrphanFilesActionError,
                        onRefresh = component::refreshOrphanFiles,
                        onOpen = component::openOrphanFile,
                        onDelete = component::deleteOrphanFile,
                        onDeleteAll = component::deleteAllOrphanFiles,
                    )
                }
            }
        }
    }
}
