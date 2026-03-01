# План: Добавить выбор периода даты/времени в StorageScreen

## Обзор
Добавить UI блок для выбора периода (начало и конец) в экран склада, используя существующий `DateTimePeriod` из `StorageComponent`.

## Файлы для изменения

### 1. `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/component/StorageComponent.kt`

**Добавить:**
- `SlotNavigation<StorageDialog>` для управления диалогами
- `childSlot` для отображения диалогов
- `createDialogChild` фабрику для создания компонентов диалогов
- `StorageDialog` sealed interface (конфигурации диалогов)
- `DialogChild` sealed interface (типы дочерних компонентов)

### 2. `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/ui/StorageScreen.kt`

**Добавить:**
- UI блок с выбором периода (2 строки для "Начало" и "Конец")
- Подписку на `component.dialog`
- Отображение `DateTimePickerDialog` при необходимости

---

## Детали реализации

### StorageComponent.kt

```kotlin
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import ru.pavlig43.core.DateTimeComponent
import kotlinx.serialization.Serializable

// Внутри класса StorageComponent:

// 1. Навигация для диалогов
private val dialogNavigation = SlotNavigation<StorageDialog>()

internal val dialog: ChildSlot<StorageDialog, DialogChild> = childSlot(
    source = dialogNavigation,
    key = "storage_dialog",
    serializer = StorageDialog.serializer(),
    handleBackButton = true,
    childFactory = ::createDialogChild
)

// 2. Конфигурация диалога
@Serializable
internal sealed interface StorageDialog {
    @Serializable
    data object StartDateTime : StorageDialog

    @Serializable
    data object EndDateTime : StorageDialog
}

// 3. Типы дочерних компонентов
sealed interface DialogChild {
    class StartDateTime(val component: DateTimeComponent) : DialogChild
    class EndDateTime(val component: DateTimeComponent) : DialogChild
}

// 4. Фабрика для создания диалогов
private fun createDialogChild(dialogConfig: StorageDialog, context: ComponentContext): DialogChild {
    val currentPeriod = dateTimePeriod.value
    return when (dialogConfig) {
        is StorageDialog.StartDateTime -> {
            DialogChild.StartDateTime(
                DateTimeComponent(
                    componentContext = context,
                    initDatetime = currentPeriod.start,
                    onChangeDate = { newDateTime ->
                        updateDateTimePeriod(currentPeriod.copy(start = newDateTime))
                    },
                    onDismissRequest = { dialogNavigation.dismiss() }
                )
            )
        }
        is StorageDialog.EndDateTime -> {
            DialogChild.EndDateTime(
                DateTimeComponent(
                    componentContext = context,
                    initDatetime = currentPeriod.end,
                    onChangeDate = { newDateTime ->
                        updateDateTimePeriod(currentPeriod.copy(end = newDateTime))
                    },
                    onDismissRequest = { dialogNavigation.dismiss() }
                )
            )
        }
    }
}

// 5. Методы для открытия диалогов (для вызова из UI)
fun openStartDateTimeDialog() = dialogNavigation.activate(StorageDialog.StartDateTime)
fun openEndDateTimeDialog() = dialogNavigation.activate(StorageDialog.EndDateTime)
```

### StorageScreen.kt

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.clock
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@Composable
fun StorageScreen(component: StorageComponent) {
    val dateTimePeriod by component.dateTimePeriod.collectAsState()
    val dialog by component.dialog.subscribeAsState()

    // Блок выбора периода
    PeriodSelectorRow(
        startDateTime = dateTimePeriod.start,
        endDateTime = dateTimePeriod.end,
        onStartClick = { component.openStartDateTimeDialog() },
        onEndClick = { component.openEndDateTimeDialog() }
    )

    // Отображение диалогов
    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.StartDateTime -> DateTimePickerDialog(dialogChild.component)
            is DialogChild.EndDateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }

    // Остальной UI...
    val loadState by component.loadState.collectAsState()
    when (val state = loadState) {
        is LoadState.Error -> ErrorScreen(state.message)
        is LoadState.Loading -> LoadingUi()
        is LoadState.Success -> {
            // ... существующий код таблицы
        }
    }
}

@Composable
private fun PeriodSelectorRow(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    {
        Text("Период:")

        DateTimeRow(
            label = "Начало",
            dateTime = startDateTime,
            onClick = onStartClick
        )

        Text("—")

        DateTimeRow(
            label = "Конец",
            dateTime = endDateTime,
            onClick = onEndClick
        )
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    dateTime: LocalDateTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolTipIconButton(
            tooltipText = label,
            onClick = onClick,
            icon = Res.drawable.clock
        )
        Text(dateTime.format(dateTimeFormat))
    }
}
```

---

## Проверка

1. Запуск приложения: `./gradlew :app:desktopApp:run`
2. Открыть экран "Склад"
3. Проверить отображение блока выбора периода
4. Кликнуть на "Начало" — должен открыться DateTimePickerDialog
5. Выбрать дату/время — период должен обновиться
6. Кликнуть на "Конец" — должен открыться DateTimePickerDialog
7. Проверить, что данные в таблице обновляются при изменении периода
8. Проверить закрытие по Back
