package ru.pavlig43.addfile.internal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import ru.pavlig43.addfile.api.data.UploadState
import ru.pavlig43.coreui.ProgressIndicator
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.coreui.tooltip.ProjectToolTip
import ru.pavlig43.theme.*

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

        if (fileUi.uploadState !is UploadState.Loading) {
            val platformFile = fileUi.platformFile
            IconButton(
                { openFile(platformFile) }) {
                Icon(Icons.Default.Search, contentDescription = null)
            }

            IconButtonToolTip(
                tooltipText = REMOVE,
                onClick = { removeFile(fileUi.composeKey) },
                icon = Icons.Default.Close
            )
        }

        when (val state = fileUi.uploadState) {
            UploadState.Loading -> ProgressIndicator(Modifier.size(24.dp))
            UploadState.Success -> ProjectToolTip(
                tooltipText = IS_UPLOAD
            ) { Icon(Icons.Default.Check, contentDescription = IS_UPLOAD) }

            is UploadState.Error -> Column {
                Text(state.message)
                IconButtonToolTip(
                    tooltipText = "${state.message} $RETRY_LOAD",
                    onClick = { retryLoadFile(fileUi.id) },
                    icon = Icons.Default.CloudDownload
                )
            }
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