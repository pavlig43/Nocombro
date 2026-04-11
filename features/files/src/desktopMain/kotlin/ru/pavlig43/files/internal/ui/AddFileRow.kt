package ru.pavlig43.files.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.extension
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.coreui.tooltip.ToolTipProject
import ru.pavlig43.files.api.model.FileUi
import ru.pavlig43.files.api.uploadState.UploadState
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.check
import ru.pavlig43.theme.cloud_download
import ru.pavlig43.theme.delete
import ru.pavlig43.theme.excel
import ru.pavlig43.theme.pdf
import ru.pavlig43.theme.search
import ru.pavlig43.theme.unknown
import ru.pavlig43.theme.word

@Composable
internal fun AddFileRow(
    fileUi: FileUi,
    openFile: (FileUi) -> Unit,
    downloadFile: (Int) -> Unit,
    hasLocalFile: Boolean,
    isDownloading: Boolean,
    removeFile: (Int) -> Unit,
    retryLoadFile: (Int) -> Unit,
    modifier: Modifier = Modifier.Companion
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

        ToolTipProject(
            tooltipText = fileUi.platformFile.absolutePath(),
        ) {
            Column(modifier = Modifier.width(250.dp)) {
                Text(
                    text = fileUi.name,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FilePresenceStatus(
                    fileUi = fileUi,
                    hasLocalFile = hasLocalFile,
                    isDownloading = isDownloading,
                )
            }
        }

        OnOpenIconButton(
            fileUi = fileUi,
            onOpenFile = openFile
        )
        DownloadIconButton(
            fileUi = fileUi,
            hasLocalFile = hasLocalFile,
            isDownloading = isDownloading,
            onDownload = downloadFile,
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
    onOpenFile: (FileUi) -> Unit
) {
    ToolTipIconButton(
        tooltipText = "Открыть",
        onClick = { onOpenFile(fileUi) },
        icon = Res.drawable.search,
        enabled = fileUi.uploadState !is UploadState.Loading,

        )
}

@Composable
private fun FilePresenceStatus(
    fileUi: FileUi,
    hasLocalFile: Boolean,
    isDownloading: Boolean,
) {
    val uploadSucceeded = fileUi.uploadState is UploadState.Success
    val statusText = when {
        fileUi.uploadState is UploadState.Error -> "Ошибка загрузки"
        isDownloading -> "Скачивается"
        hasLocalFile && uploadSucceeded -> "Локально и загружен"
        !hasLocalFile && uploadSucceeded -> "Загружен, локальной копии нет"
        hasLocalFile && fileUi.remoteObjectKey != null -> "Локально, есть remote key"
        hasLocalFile -> "Локально"
        fileUi.remoteObjectKey != null -> "Локально нет, есть remote key"
        else -> "Нет локальной копии"
    }
    val statusColor = when {
        fileUi.uploadState is UploadState.Error -> MaterialTheme.colorScheme.error
        isDownloading -> MaterialTheme.colorScheme.primary
        hasLocalFile -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Text(
        text = statusText,
        color = statusColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun DownloadIconButton(
    fileUi: FileUi,
    hasLocalFile: Boolean,
    isDownloading: Boolean,
    onDownload: (Int) -> Unit,
) {
    val canDownload = fileUi.remoteObjectKey != null && !hasLocalFile
    if (!canDownload) {
        return
    }
    if (isDownloading) {
        LoadingUi(Modifier.size(24.dp))
        return
    }
    ToolTipIconButton(
        tooltipText = "Скачать локальную копию",
        onClick = { onDownload(fileUi.composeKey) },
        icon = Res.drawable.cloud_download,
        enabled = fileUi.uploadState !is UploadState.Loading,
    )
}

@Composable
private fun RemoveIconButton(
    fileUi: FileUi,
    removeFile: (Int) -> Unit
) {
    ToolTipIconButton(
        tooltipText = "Удалить вложение из записи",
        enabled = fileUi.uploadState !is UploadState.Loading,
        onClick = { removeFile(fileUi.composeKey) },
        icon = Res.drawable.delete
    )

}

@Composable
private fun UploadIcon(
    fileUi: FileUi,
    retryLoadFile: (Int) -> Unit
) {
    when (val state = fileUi.uploadState) {
        UploadState.Loading -> LoadingUi(Modifier.size(24.dp))
        UploadState.Success -> ToolTipProject(
            tooltipText = "Загружено"
        ) { Icon(painterResource(Res.drawable.check), contentDescription = null) }

        is UploadState.Error -> Column {
            Text(state.message)
            ToolTipIconButton(
                tooltipText = "${state.message} Повторить загрузку",
                onClick = { retryLoadFile(fileUi.composeKey) },
                icon = Res.drawable.cloud_download
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
