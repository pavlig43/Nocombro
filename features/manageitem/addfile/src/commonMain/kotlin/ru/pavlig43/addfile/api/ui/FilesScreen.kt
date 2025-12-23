package ru.pavlig43.addfile.api.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import ru.pavlig43.addfile.api.component.IFilesComponent
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.addfile.internal.fileopener.FileOpener
import ru.pavlig43.addfile.internal.fileopener.getFileOpener
import ru.pavlig43.addfile.internal.ui.*
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen

@Composable
fun FilesScreen(
    component: IFilesComponent,
    modifier: Modifier = Modifier
) {
    val addedFiles by component.filesUi.collectAsState()

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        LoadInitDataScreen(component.loadInitDataComponent) {
            AddFileBody(
                addedFiles = addedFiles,
                addFilePathAndName = component::addFilePath,
                removeFile = component::removeFile,
                retryLoadFile = component::retryLoadFile,
                modifier = it

            )
        }
    }

}

@Composable
private fun AddFileBody(
    addedFiles: List<FileUi>,
    addFilePathAndName: (String) -> Unit,
    removeFile: (Int) -> Unit,
    retryLoadFile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    var showDialogState by remember { mutableStateOf(false) }
    var existFilePath by remember { mutableStateOf("") }
    val launcher = rememberFilePickerLauncher { platformFile: PlatformFile? ->

        platformFile?.let {
            val innerFile = PlatformFile(FileKit.filesDir, it.name)
            if (innerFile.exists()) {
                existFilePath = innerFile.absolutePath()
                showDialogState = true
            } else {
                addFilePathAndName(platformFile.path)
            }
        }
    }

    Column(
        modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.outline),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showDialogState) {
            ExistFileDialog(
                onDismissRequest = { showDialogState = false },
                filePath = existFilePath
            )
        } else {
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
            if (addedFiles.isEmpty()) {
                Text(REQUIRED_FILE_ADD, color = MaterialTheme.colorScheme.error)
            }
            addedFiles.forEach { file ->
                val fileOpener: FileOpener = getFileOpener()
                AddFileRow(
                    removeFile = removeFile,
                    fileUi = file,
                    openFile = {
                        fileOpener.openFile(it.absolutePath())
                    },
                    retryLoadFile = retryLoadFile
                )
            }
        }


    }
}




