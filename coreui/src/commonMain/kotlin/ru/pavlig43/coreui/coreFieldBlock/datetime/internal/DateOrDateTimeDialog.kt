package ru.pavlig43.coreui.coreFieldBlock.datetime.internal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
internal fun DateOrDateTimeDialog(
    initialDateTime: Long,
    onConfirm: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    dateTimePicker: @Composable (
        dateTime: Long,
        timeZone: TimeZone,
        onChange: (Long) -> Unit
    ) -> Unit,
) {
    var dateTime by remember { mutableLongStateOf(initialDateTime) }
    val timeZone = TimeZone.currentSystemDefault()

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        // Полупрозрачный фон + «карточка» по центру
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Сам пикер
                dateTimePicker(
                    dateTime,
                    timeZone
                ) { dateTime = it }

                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Большие кнопки
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Отмена",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Button(
                        onClick = {
                            onConfirm(dateTime)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "OK",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
