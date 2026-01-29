package ru.pavlig43.files.api.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.files.api.component.FilesComponent
import ru.pavlig43.files.api.model.FileUi
import ru.pavlig43.files.internal.ui.AddFileRow
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.add_circle
import ru.pavlig43.theme.menu
import ru.pavlig43.theme.warning


@Composable
fun FilesScreen(
    component: FilesComponent,
    modifier: Modifier = Modifier
) {
    val files by component.filesUi.collectAsState()

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        LoadInitDataScreen(component.initDataComponent) {
            AddFileBody(
                files = files,
                addPlatformFile = component::addNewFile,
                removeFile = component::removeFile,
                retryLoadFile = component::retryLoadFile,
                calculateNocombroFileName = component::calculateNocombroFileName,
                modifier = it

            )
        }
    }

}

/**
 * Компонент для добавления и управления файлами в UI.
 *
 * Отображает список файлов, предоставляет возможность добавления новых файлов через системный файл-пикер,
 * обрабатывает дубликаты файлов и позволяет управлять существующими файлами (удаление, повторная загрузка).
 *
 * @param files Список отображаемых файлов типа [FileUi]
 * @param addPlatformFile Callback для добавления нового файла. Вызывается когда пользователь выбирает файл
 *                        и этот файл не является дубликатом существующего
 * @param calculateNocombroFileName Функция для вычисления имени файла .
 *                                  Используется для проверки дубликатов
 * @param removeFile Callback для удаления файла из стэйта ui по его composeKey
 * @param retryLoadFile Callback для повторной загрузки файла по его composeKey (например, при ошибке загрузки)
 *
 * ## Логика работы:
 * 1. **Добавление файла**: Пользователь выбирает файл через системный файл-пикер
 * 2. **Проверка дубликатов**: Если файл с именем Nocombro уже существует, показывается диалог подтверждения.
 * Если пользователь да, то файл перезаписывается.
 *
 * @see FileUi Класс-модель для отображения файла
 * @see PlatformFile Платформо-специфичное представление файла
 * @see DuplicateFileDialog Диалоговое окно для подтверждения перезаписи дубликата
 * @see rememberFilePickerLauncher Launcher для выбора файлов
 */
@Composable
private fun AddFileBody(
    files: List<FileUi>,
    addPlatformFile: (PlatformFile) -> Unit,
    calculateNocombroFileName: (PlatformFile) -> String,
    removeFile: (Int) -> Unit,
    retryLoadFile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Состояние для управления диалогом дубликатов файлов
    var isDuplicateFileDialog by remember { mutableStateOf(false) }

    // Файл, ожидающий подтверждения добавления (при обнаружении дубликата)
    var pendingDuplicateFile by remember { mutableStateOf<PlatformFile?>(null) }

    // Ключ дублирующегося файла для возможного удаления
    var duplicateComposeKey by remember { mutableStateOf(0) }

    // Диалог подтверждения перезаписи дубликата
    if (isDuplicateFileDialog) {
        DuplicateFileDialog(
            onDismissRequest = { isDuplicateFileDialog = false },
            onAddPlatformFile = {
                pendingDuplicateFile?.let { file ->
                    // Удаляем старый файл и добавляем новый
                    removeFile(duplicateComposeKey)
                    addPlatformFile(file)
                }
            }
        )
    }

    // Launcher для выбора файлов через системный файл-пикер
    val launcher = rememberFilePickerLauncher { platformFile: PlatformFile? ->
        platformFile?.let { selectedFile ->
            // Проверяем, не является ли выбранный файл дубликатом
            val duplicateFile = files.firstOrNull { file ->
                file.name == calculateNocombroFileName(selectedFile)
            }

            if (duplicateFile != null) {
                // Найден дубликат - показываем диалог подтверждения
                duplicateComposeKey = duplicateFile.composeKey
                pendingDuplicateFile = selectedFile
                isDuplicateFileDialog = true
            } else {
                // Дубликатов нет - добавляем файл напрямую
                addPlatformFile(selectedFile)
            }
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
            Text("Файлы")
            ToolTipIconButton(
                tooltipText = "Добавить файл",
                onClick = { launcher.launch() },
                icon = Res.drawable.add_circle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFileDialog(
    onDismissRequest: () -> Unit,
    onAddPlatformFile: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Иконка
                Icon(
                    painter = painterResource(Res.drawable.warning) ,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.error
                )

                // Заголовок
                Text(
                    text = """
                    Файл c таким именем уже существует на строке.
                    Если перезапишешь,то старая строка удалится и добавится новая в конец списка.
                """.trimIndent(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Текст
                Text(
                    text = "Перезаписать существующий файл?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Отмена")
                    }
                    Button(onClick = {
                        onAddPlatformFile()
                        onDismissRequest()
                    }) {
                        Text("Перезаписать")
                    }
                }
            }
        }
    )


}


