package ru.pavlig43.addfile.api.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.name
import org.jetbrains.compose.resources.DrawableResource
import ru.pavlig43.addfile.api.component.IAddFileComponent
import ru.pavlig43.addfile.internal.ui.ADD_FILE
import ru.pavlig43.addfile.internal.ui.AddFileRow
import ru.pavlig43.addfile.internal.ui.ExistFileDialog
import ru.pavlig43.addfile.internal.ui.FILES
import ru.pavlig43.addfile.internal.ui.REQUIRED_FILE_ADD
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.excel
import ru.pavlig43.theme.pdf
import ru.pavlig43.theme.unknown
import ru.pavlig43.theme.word

@Composable
 fun AddFileScreen(
    component: IAddFileComponent,
    modifier: Modifier = Modifier
) {

    val addedFiles by component.addedFiles.collectAsState()
    var showDialogState by remember { mutableStateOf(false) }
    var existFilePath by remember { mutableStateOf("") }

    val launcher = rememberFilePickerLauncher { platformFile: PlatformFile? ->

        platformFile?.let {
            val innerFile = PlatformFile(FileKit.filesDir, it.name)
            if (innerFile.exists()) {
                existFilePath = innerFile.absolutePath()
                showDialogState = true

            } else {
                component.addFile(platformFile)
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
                AddFileRow(
                    removeFile = component::removeFile,
                    addedFile = file,
                    openFile = component::openFile,
                    retryLoadFile = component::retryLoadFile
                )
            }
        }


    }
}

internal fun String.toIconDrawableResource(): DrawableResource {

    return when (this) {
        "pdf" -> Res.drawable.pdf
        "docx" -> Res.drawable.word
        "xlsx" -> Res.drawable.excel
        "jpg" -> Res.drawable.unknown
        "png" -> Res.drawable.unknown
        else -> Res.drawable.unknown
    }
}

