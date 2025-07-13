package ru.pavlig43.addfile.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.addfile.api.data.UploadState
import ru.pavlig43.addfile.api.ui.toIconDrawableResource
import ru.pavlig43.coreui.ProgressIndicator
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.coreui.tooltip.ProjectToolTip

@Composable
internal fun AddFileRow(
    addedFile: AddedFile,
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
            painterResource(addedFile.platformFile.extension.toIconDrawableResource()),
            contentDescription = null,
            Modifier.size(36.dp),
            tint = Color.Unspecified
        )

        ProjectToolTip(
            tooltipText = addedFile.platformFile.name,
        ) {
            Text(
                text = addedFile.platformFile.name,
                textDecoration = TextDecoration.Underline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(250.dp),

                )
        }




        if (addedFile.uploadState !is UploadState.Loading) {
            IconButton(
                { openFile(addedFile.platformFile) }) {
                Icon(Icons.Default.Search, contentDescription = null)
            }

            IconButtonToolTip(
                tooltipText = REMOVE,
                onClick = { removeFile(addedFile.index) },
                icon = Icons.Default.Close
            )
        }

        when (addedFile.uploadState) {
            UploadState.Loading -> ProgressIndicator(Modifier.size(24.dp))
            UploadState.Success -> ProjectToolTip(
                tooltipText = IS_UPLOAD
            ) { Icon(Icons.Default.Check, contentDescription = IS_UPLOAD) }

            UploadState.Error -> IconButtonToolTip(
                tooltipText = RETRY_LOAD_FILE,
                onClick = { retryLoadFile(addedFile.index) },
                icon = Icons.Default.CloudDownload
            )
        }

    }
}