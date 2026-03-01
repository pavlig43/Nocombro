# План: Карточка с отрицательными партиями в таблице склада

## Обзор
Добавить Card с LazyColumn между `PeriodSelectorRow` и таблицей. Карточка показывает список всех партий с отрицательными значениями (без повторений, только партии, не продукты). При клике на элемент — скролл к соответствующей строке в таблице.

---

## Файлы для изменения

### 1. `StorageScreen.kt` (основные изменения)

**Путь:** `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/ui/StorageScreen.kt`

---

## Реализация

### Шаг 1: Создание data class для отрицательной партии

```kotlin
private data class NegativeBatchItem(
    val index: Int,           // Индекс в таблице
    val itemId: Int,          // ID партии
    val itemName: String      // Название
)
```

### Шаг 2: Вычисление списка отрицательных партий

```kotlin
private fun getNegativeBatches(tableData: StorageTableData): List<NegativeBatchItem> {
    return tableData.displayedProducts
        .mapIndexedNotNull { index, item ->
            // Только партии, не продукты
            if (!item.isProduct) return@mapIndexedNotNull null

            val hasNegative = item.balanceBeforeStart < 0 ||
                            item.incoming < 0 ||
                            item.outgoing < 0 ||
                            item.balanceOnEnd < 0

            if (hasNegative) {
                NegativeBatchItem(index, item.itemId, item.itemName)
            } else null
        }
}
```

### Шаг 3: Создание компонента карточки

```kotlin
@Composable
private fun NegativeBatchesCard(
    negativeBatches: ImmutableList<NegativeBatchItem>,
    onBatchClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Партии с отрицательными значениями (${negativeBatches.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Список с фиксированной высотой
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = negativeBatches,
                    key = { it.itemId }
                ) { batch ->
                    NegativeBatchItem(
                        item = batch,
                        onClick = { onBatchClick(batch.index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NegativeBatchItem(
    item: NegativeBatchItem,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Text(
            text = item.itemName,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

### Шаг 4: Интеграция в StorageScreen

Вынести `verticalState` и добавить карточку:

```kotlin
@Composable
fun StorageScreen(component: StorageComponent) {
    // ... существующий код до loadState ...

    val loadState by component.loadState.collectAsState()
    when (val state = loadState) {
        is LoadState.Success -> {
            val tableData by component.tableData.collectAsState()

            // Вычисляем список отрицательных партий
            val negativeBatches = remember(tableData) {
                getNegativeBatches(tableData).toImmutableList()
            }

            // Выносим verticalState на уровень экрана
            val verticalState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            // Существующий PeriodSelectorRow
            PeriodSelectorRow(...)

            // НОВАЯ КАРТОЧКА (показываем только если есть отрицательные партии)
            if (negativeBatches.isNotEmpty()) {
                NegativeBatchesCard(
                    negativeBatches = negativeBatches,
                    onBatchClick = { index ->
                        coroutineScope.launch {
                            verticalState.animateScrollToItem(index)
                        }
                    }
                )
            }

            // Текст периода
            Text("Выбранный период ...")

            // ... остальной код ...

            // Передаём verticalState в таблицу
            StorageTable(
                // ...
                verticalState = verticalState,
                // ...
            )
        }
    }
}
```

### Шаг 5: Обновить StorageTable для принятия verticalState снаружи

Изменить сигнатуру функции:

```kotlin
@Composable
private fun StorageTable(
    state: TableState<StorageProductField>,
    tableData: StorageTableData,
    columns: ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, StorageTableData>>,
    verticalState: LazyListState,  // Добавляем параметр
    modifier: Modifier = Modifier,
) {
    // Убираем val verticalState = rememberLazyListState()
    // Используем переданный verticalState
}
```

---

## Структура изменений

```
StorageScreen.kt:
  - Добавить data class NegativeBatchItem и NegativeField
  - Добавить функцию getNegativeBatches()
  - Добавить NegativeBatchesCard() компонент
  - Добавить NegativeBatchItem() компонент (элемент списка)
  - Вынести verticalState из StorageTable в StorageScreen
  - Вставить NegativeBatchesCard между PeriodSelectorRow и текстом периода
  - Обновить StorageTable для принятия verticalState как параметра
```

---

## Необходимые импорты

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.warning
```

---

## Проверка

1. Запустить приложение: `./gradlew :app:desktopApp:run`
2. Открыть экран склада
3. Создать данные с отрицательными значениями в партиях (если их нет)
4. Проверить:
   - Card появляется только если есть партии с отрицательными значениями
   - В списке только партии (isProduct = true), без продуктов
   - Нет дубликатов партий
   - Счётчик в заголовке показывает правильное количество
   - При клике на элемент происходит скролл к строке в таблице
   - LazyColumn имеет ограниченную высоту (max = 200.dp)
   - При изменении данных список пересчитывается

---

## Визуальный вид карточки

```
┌─────────────────────────────────────────────────────┐
│ ⚠ Партии с отрицательными значениями (3)           │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Партия Молоко 3.2%                                │
│  Партия Сыр Российский                              │
│  Партия Хлеб белый                                  │
│                                                     │
└─────────────────────────────────────────────────────┘
```
