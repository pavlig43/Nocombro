package ru.pavlig43.addfile.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.addfile.api.data.RemoveState
import ru.pavlig43.addfile.api.data.UploadState
import ru.pavlig43.coreui.ProgressIndicator
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.coreui.tooltip.ProjectToolTip
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.excel
import ru.pavlig43.theme.pdf
import ru.pavlig43.theme.unknown
import ru.pavlig43.theme.word


@Composable
internal fun AddFileRow(
    fileUi: FileUi,
    openFile: (PlatformFile) -> Unit,
    removeFile: (Int) -> Unit,
    retryLoadFile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painterResource(fileUi.platformFile.extension.toIconDrawableResource()),
            contentDescription = null,
            Modifier.size(36.dp),
            tint = Color.Unspecified
        )

        ProjectToolTip(
            tooltipText = fileUi.platformFile.absolutePath(),
        ) {
            Text(
                text = fileUi.platformFile.name,
                textDecoration = TextDecoration.Underline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(250.dp),

                )
        }

        OnOpenIconButton(
            fileUi = fileUi,
            onOpenFile = openFile
        )

        RemoveIconButton(
            fileUi = fileUi,
            removeFile = removeFile
        )
        UploadIcon(
            fileUi = fileUi,
            retryLoadFile = retryLoadFile
        )

    }
}

@Composable
private fun OnOpenIconButton(
    fileUi: FileUi,
    onOpenFile: (PlatformFile) -> Unit
) {
    IconButtonToolTip(
        tooltipText = "Открыть",
        onClick = { onOpenFile(fileUi.platformFile) },
        icon = Icons.Default.Search,
        enabled = fileUi.uploadState !is UploadState.Loading && fileUi.removeState !is RemoveState.InProgress,

        )
}

@Composable
private fun RemoveIconButton(
    fileUi: FileUi,
    removeFile: (Int) -> Unit
) {
    when (val state = fileUi.removeState) {
        is RemoveState.Error -> Text(state.message)
        is RemoveState.InProgress -> ProgressIndicator(Modifier.size(24.dp))
        is RemoveState.Init -> IconButtonToolTip(
            tooltipText = "Удалить",
            enabled = fileUi.uploadState !is UploadState.Loading,
            onClick = { removeFile(fileUi.composeKey) },
            icon = Icons.Default.Close
        )
    }
}

@Composable
private fun UploadIcon(
    fileUi: FileUi,
    retryLoadFile: (Int) -> Unit
) {
    when (val state = fileUi.uploadState) {
        UploadState.Loading -> ProgressIndicator(Modifier.size(24.dp))
        UploadState.Success -> ProjectToolTip(
            tooltipText = IS_UPLOAD
        ) { Icon(Icons.Default.Check, contentDescription = IS_UPLOAD) }

        is UploadState.Error -> Column {
            Text(state.message)
            IconButtonToolTip(
                tooltipText = "${state.message} $RETRY_LOAD",
                enabled = fileUi.removeState !is RemoveState.InProgress,
                onClick = { retryLoadFile(fileUi.composeKey) },
                icon = Icons.Default.CloudDownload
            )
        }
    }
}

private fun String.toIconDrawableResource(): DrawableResource {

    return when (this) {
        "pdf" -> Res.drawable.pdf
        "docx" -> Res.drawable.word
        "xlsx" -> Res.drawable.excel
        "jpg" -> Res.drawable.unknown
        "png" -> Res.drawable.unknown
        else -> Res.drawable.unknown
    }
}