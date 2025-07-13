package ru.pavlig43.addfile.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch
import ru.pavlig43.coreui.tooltip.ProjectToolTip

@Composable
internal fun ExistFileDialog(
    onDismissRequest: () -> Unit,
    filePath: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clickBoardManager = LocalClipboard.current
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

        Text(FILE_EXISTS_CONFIRMATION_MESSAGE)
        ProjectToolTip(
            tooltipText = COPY_TEXT
        ) {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        //TODO сделать текст в ToolTip  ,где пишется скопировано
                        clickBoardManager.copyTextToClipBoard(filePath)
                    }
                }
            ) {
                Text(text = filePath)
            }
        }


        Button(onClick = onDismissRequest) {
            Text(text = "ОК")
        }
    }
}