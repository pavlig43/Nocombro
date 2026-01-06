package ru.pavlig43.addfile.api.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import ru.pavlig43.addfile.api.component.FilesComponent
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.addfile.internal.ui.ADD_FILE
import ru.pavlig43.addfile.internal.ui.AddFileRow
import ru.pavlig43.addfile.internal.ui.FILES
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen

@Composable
fun FilesScreen(
    component: FilesComponent<*>,
    modifier: Modifier = Modifier
) {
    val files by component.filesUi.collectAsState()

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        LoadInitDataScreen(component.loadInitDataComponent) {
            AddFileBody(
                files = files,
                addPlatformFile = component::addPlatformPath,
                removeFile = component::removeFile,
                retryLoadFile = component::retryLoadFile,
                modifier = it

            )
        }
    }

}

@Composable
private fun AddFileBody(
    files: List<FileUi>,
    addPlatformFile: (PlatformFile) -> Unit,
    removeFile: (Int) -> Unit,
    retryLoadFile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    val launcher = rememberFilePickerLauncher { platformFile: PlatformFile? ->

        platformFile?.let {
            addPlatformFile(platformFile)

        }
    }

    Column(
        modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.outline),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(FILES)
            IconButtonToolTip(
                tooltipText = ADD_FILE,
                onClick = { launcher.launch() },
                icon = Icons.Default.AddCircle
            )
        }
        files.forEach { file ->
            AddFileRow(
                removeFile = removeFile,
                fileUi = file,
                openFile = {
                    FileKit.openFileWithDefaultApplication(
                        file = it,
                    )
                },
                retryLoadFile = retryLoadFile
            )
        }
    }


}





